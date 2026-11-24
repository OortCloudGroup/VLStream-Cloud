using System;
using System.Diagnostics;
using System.Drawing;
using System.IO;
using System.Reflection;
using System.Windows.Forms;
using Microsoft.Web.WebView2.Core;
using Microsoft.Web.WebView2.WinForms;

[assembly: AssemblyTitle("VLStream")]
[assembly: AssemblyProduct("VLStream Cloud")]
[assembly: AssemblyCompany("VLStream")]
[assembly: AssemblyVersion("1.2.12.0")]
[assembly: AssemblyFileVersion("1.2.12.0")]

internal static class Program
{
    [STAThread]
    private static void Main()
    {
        Application.EnableVisualStyles();
        Application.SetCompatibleTextRenderingDefault(false);
        Application.Run(new ConsoleWindow());
    }
}

internal sealed class ConsoleWindow : Form
{
    private static readonly Uri ConsoleUrl = new Uri("http://localhost:17880/bus/vls-ui/");
    private const string WebView2DownloadUrl = "https://developer.microsoft.com/microsoft-edge/webview2/#download-section";

    private readonly WebView2 browser = new WebView2();
    private readonly ToolStripStatusLabel status = new ToolStripStatusLabel();
    private readonly ToolStripButton reload = new ToolStripButton("重新加载");
    private readonly ToolStripButton installRuntime = new ToolStripButton("下载 WebView2 运行时");
    private readonly Timer reconnect = new Timer();
    private CoreWebView2Environment environment;

    public ConsoleWindow()
    {
        Text = "VLStream";
        StartPosition = FormStartPosition.CenterScreen;
        MinimumSize = new Size(700, 480);
        Size = new Size(1280, 850);
        AutoScaleMode = AutoScaleMode.Dpi;

        browser.Dock = DockStyle.Fill;
        var toolbar = new ToolStrip { GripStyle = ToolStripGripStyle.Hidden };
        toolbar.Items.Add(reload);
        toolbar.Items.Add(installRuntime);
        toolbar.Items.Add(status);
        Controls.Add(browser);
        Controls.Add(toolbar);

        installRuntime.Visible = false;
        installRuntime.Click += (sender, args) => OpenExternal(new Uri(WebView2DownloadUrl));
        reload.Enabled = false;
        reload.Click += (sender, args) => Reload();
        reconnect.Interval = 5000;
        reconnect.Tick += (sender, args) => NavigateToConsole();
        status.Text = "正在加载 VLStream…";
        Shown += InitializeBrowser;
        FormClosed += (sender, args) => reconnect.Dispose();
    }

    private async void InitializeBrowser(object sender, EventArgs args)
    {
        try
        {
            string profile = Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData),
                "VLStream", "WebView2");
            environment = await CoreWebView2Environment.CreateAsync(userDataFolder: profile);
            if (IsDisposed)
            {
                return;
            }

            await browser.EnsureCoreWebView2Async(environment);
            if (IsDisposed)
            {
                return;
            }

            ConnectEvents(browser.CoreWebView2);
            browser.CoreWebView2.NavigationCompleted += OnNavigationCompleted;
            installRuntime.Visible = false;
            reload.Enabled = true;
            NavigateToConsole();
        }
        catch (WebView2RuntimeNotFoundException)
        {
            if (!IsDisposed)
            {
                status.Text = "此电脑需要 Microsoft Edge WebView2 运行时。安装后可重新加载。";
                installRuntime.Visible = true;
                reload.Enabled = true;
            }
        }
        catch (Exception error)
        {
            if (!IsDisposed)
            {
                status.Text = "无法启动内置浏览器。请重试或重新打开 VLStream。";
                status.ToolTipText = error.Message;
                reload.Enabled = true;
            }
        }
    }

    private void Reload()
    {
        if (browser.CoreWebView2 == null)
        {
            reload.Enabled = false;
            InitializeBrowser(this, EventArgs.Empty);
            return;
        }

        if (reconnect.Enabled || browser.Source == null)
        {
            NavigateToConsole();
            return;
        }

        browser.Reload();
    }

    private void NavigateToConsole()
    {
        reconnect.Stop();
        status.Text = "正在连接本机服务…";
        status.ToolTipText = ConsoleUrl.AbsoluteUri;
        browser.CoreWebView2.Navigate(ConsoleUrl.AbsoluteUri);
    }

    private void OnNavigationCompleted(object sender, CoreWebView2NavigationCompletedEventArgs args)
    {
        if (args.IsSuccess)
        {
            reconnect.Stop();
            status.Text = String.Empty;
        }
        else if (args.WebErrorStatus != CoreWebView2WebErrorStatus.OperationCanceled)
        {
            status.Text = "本机服务暂时无法访问。客户端将自动重试，也可点击重新加载。";
            reconnect.Start();
        }
    }

    private void ConnectEvents(CoreWebView2 core)
    {
        core.NavigationStarting += OnNavigationStarting;
        core.NewWindowRequested += OnNewWindowRequested;
    }

    private static bool IsConsoleUrl(Uri address)
    {
        return address.Scheme == ConsoleUrl.Scheme &&
            address.Host == ConsoleUrl.Host && address.Port == ConsoleUrl.Port;
    }

    private static void OnNavigationStarting(object sender, CoreWebView2NavigationStartingEventArgs args)
    {
        Uri address;
        if (Uri.TryCreate(args.Uri, UriKind.Absolute, out address))
        {
            if (IsConsoleUrl(address) || address.AbsoluteUri == "about:blank")
            {
                return;
            }

            if (address.Scheme == Uri.UriSchemeHttps)
            {
                OpenExternal(address);
            }
        }

        args.Cancel = true;
    }

    private async void OnNewWindowRequested(object sender, CoreWebView2NewWindowRequestedEventArgs args)
    {
        Uri address;
        args.Handled = true;
        if (!Uri.TryCreate(args.Uri, UriKind.Absolute, out address))
        {
            return;
        }

        if (!IsConsoleUrl(address) && address.AbsoluteUri != "about:blank")
        {
            if (address.Scheme == Uri.UriSchemeHttps)
            {
                OpenExternal(address);
            }
            return;
        }

        var popup = new Form { Text = "VLStream", StartPosition = FormStartPosition.CenterParent, Size = new Size(1100, 750) };
        var webView = new WebView2 { Dock = DockStyle.Fill };
        popup.Controls.Add(webView);
        using (var deferral = args.GetDeferral())
        {
            try
            {
                await webView.EnsureCoreWebView2Async(environment);
                if (IsDisposed)
                {
                    popup.Dispose();
                    return;
                }

                ConnectEvents(webView.CoreWebView2);
                webView.CoreWebView2.WindowCloseRequested += (window, closeArgs) => popup.Close();
                args.NewWindow = webView.CoreWebView2;
                popup.Show(this);
            }
            catch (Exception)
            {
                popup.Dispose();
                if (!IsDisposed)
                {
                    MessageBox.Show(this, "无法打开新窗口，请重试。", "VLStream", MessageBoxButtons.OK, MessageBoxIcon.Warning);
                }
            }
        }
    }

    private static void OpenExternal(Uri address)
    {
        try
        {
            Process.Start(new ProcessStartInfo(address.AbsoluteUri) { UseShellExecute = true });
        }
        catch (System.ComponentModel.Win32Exception)
        {
            MessageBox.Show("无法打开系统浏览器。请检查 Windows 的默认浏览器设置。", "VLStream",
                MessageBoxButtons.OK, MessageBoxIcon.Warning);
        }
    }
}
