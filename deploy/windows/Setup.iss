#ifndef DeliveryRoot
  #error Pass the absolute delivery directory with /DDeliveryRoot.
#endif
#ifndef SetupAppId
  #define SetupAppId "{{0A15FD16-A008-4816-BCFE-1451EA6606EB}"
#endif
#ifndef SetupGroupName
  #define SetupGroupName "VLStream"
#endif
#ifndef ChineseLanguageFile
  #define ChineseLanguageFile "compiler:Languages\ChineseSimplified.isl"
#endif

[Setup]
AppId={#SetupAppId}
AppName=VLStream
AppVersion=1.2.12
AppPublisher=OortCloudGroup
DefaultDirName={localappdata}\Programs\VLStream
DefaultGroupName={#SetupGroupName}
DisableWelcomePage=no
DisableProgramGroupPage=yes
PrivilegesRequired=lowest
MinVersion=10.0.19045
ArchitecturesAllowed=x64os
ArchitecturesInstallIn64BitMode=x64os
UninstallDisplayIcon={app}\VLStream.exe
OutputDir={#DeliveryRoot}
OutputBaseFilename=VLStream-Cloud-v1.2.12-Windows-x64-Native-Setup
Compression=lzma2/fast
SolidCompression=yes
WizardStyle=modern
SetupLogging=yes
ShowLanguageDialog=no

[Languages]
Name: "zhcn"; MessagesFile: "{#ChineseLanguageFile}"

[Messages]
zhcn.WelcomeLabel2=此向导将安装 VLStream 桌面客户端，并在本机初始化所需服务。%n%n首次安装可能需要几分钟。请等待出现“安装完成”再打开客户端。
zhcn.FinishedLabelNoIcons=VLStream 已安装，本机服务已就绪。%n%n新安装的初始账号为 admin，密码为 123456。请在首次登录后修改密码。
zhcn.FinishedLabel=VLStream 已安装，本机服务已就绪。%n%n新安装的初始账号为 admin，密码为 123456。请在首次登录后修改密码。

[Tasks]
Name: "desktopicon"; Description: "创建 VLStream 桌面快捷方式"

[Files]
Source: "{#DeliveryRoot}\payload\*"; DestDir: "{app}"; Excludes: "*.pdb,*.lib,*.exp,*.log,*.tmp"; Flags: ignoreversion recursesubdirs createallsubdirs

[Icons]
Name: "{group}\VLStream"; Filename: "{app}\VLStream.exe"; WorkingDir: "{app}"
Name: "{group}\VLStream 安装说明"; Filename: "{sys}\notepad.exe"; Parameters: """{app}\Windows安装说明.md"""
Name: "{userdesktop}\VLStream"; Filename: "{app}\VLStream.exe"; WorkingDir: "{app}"; Tasks: desktopicon

[UninstallDelete]
Type: files; Name: "{app}\VLStreamDataRoot.txt"

[Code]
const
  WebView2Key = 'SOFTWARE\Microsoft\EdgeUpdate\Clients\{F3017226-FE2A-4295-8BDF-00C3A9A7E4C5}';
  VcRuntimeKey = 'SOFTWARE\Microsoft\VisualStudio\14.0\VC\Runtimes\x64';

var
  ProvisioningPage: TOutputMarqueeProgressWizardPage;
  ProvisioningSucceeded: Boolean;

function DefaultDataRoot: String;
begin
  Result := ExpandConstant('{localappdata}\VLStream\NativeData');
end;

function IsValidDataRoot(const Value: String): Boolean;
var
  FullPath: String;
  AppPath: String;
begin
  Result := False;
  if Length(Value) < 4 then
    exit;
  if (Copy(Value, 2, 2) <> ':\') or (Pos('"', Value) <> 0) or
    (Pos(#13, Value) <> 0) or (Pos(#10, Value) <> 0) then
    exit;
  FullPath := AddBackslash(ExpandFileName(Value));
  AppPath := AddBackslash(ExpandConstant('{app}'));
  Result := (Length(FullPath) > 3) and (CompareText(Copy(FullPath, 1, Length(AppPath)), AppPath) <> 0);
end;

function ExistingDataRoot: String;
var
  Values: TArrayOfString;
begin
  Result := DefaultDataRoot;
  if LoadStringsFromFile(ExpandConstant('{app}\VLStreamDataRoot.txt'), Values) then
    if GetArrayLength(Values) = 1 then
      if IsValidDataRoot(Trim(Values[0])) then
        Result := RemoveBackslashUnlessRoot(ExpandFileName(Trim(Values[0])));
end;

function SelectedDataRoot: String;
var
  DataOverride: String;
begin
  DataOverride := Trim(ExpandConstant('{param:VLSDataRoot|}'));
  if DataOverride = '' then
  begin
    Result := ExistingDataRoot;
    exit;
  end;
  if not IsValidDataRoot(DataOverride) then
    RaiseException('服务数据目录必须是安装目录以外的本机磁盘绝对路径。');
  Result := RemoveBackslashUnlessRoot(ExpandFileName(DataOverride));
end;

function HasWebView2: Boolean;
var
  RuntimeVersion: String;
begin
  Result := (RegQueryStringValue(HKLM32, WebView2Key, 'pv', RuntimeVersion) and
    (RuntimeVersion <> '') and (RuntimeVersion <> '0.0.0.0')) or
    (RegQueryStringValue(HKCU, WebView2Key, 'pv', RuntimeVersion) and
    (RuntimeVersion <> '') and (RuntimeVersion <> '0.0.0.0'));
end;

function HasVcRuntimeInView(const RootKey: Integer): Boolean;
var
  Installed: Cardinal;
  Major: Cardinal;
  Minor: Cardinal;
begin
  Result := RegQueryDWordValue(RootKey, VcRuntimeKey, 'Installed', Installed) and (Installed = 1) and
    RegQueryDWordValue(RootKey, VcRuntimeKey, 'Major', Major) and
    RegQueryDWordValue(RootKey, VcRuntimeKey, 'Minor', Minor) and
    ((Major > 14) or ((Major = 14) and (Minor >= 29)));
end;

function HasVcRuntime: Boolean;
begin
  Result := HasVcRuntimeInView(HKLM64) or HasVcRuntimeInView(HKLM32);
end;

function RunNative(const Action, DataRoot: String): String;
var
  ExitCode: Integer;
  Parameters: String;
begin
  Result := '';
  Parameters := '-NoLogo -NoProfile -NonInteractive -ExecutionPolicy RemoteSigned -File ' +
    AddQuotes(ExpandConstant('{app}\native\Install-VLStream.ps1')) + ' -Action ' + Action +
    ' -InstallRoot ' + AddQuotes(ExpandConstant('{app}')) + ' -DataRoot ' + AddQuotes(DataRoot);
  if not ExecAndLogOutput(ExpandConstant('{app}\runtime\pwsh\pwsh.exe'), Parameters,
    ExpandConstant('{app}'), SW_HIDE, ewWaitUntilTerminated, ExitCode, nil) then
    Result := '无法运行 VLStream 本机服务程序。' + #13#10 + SysErrorMessage(ExitCode)
  else if ExitCode <> 0 then
    Result := 'VLStream 本机服务程序退出，错误代码 ' + IntToStr(ExitCode) + '。';
end;

function PrepareToInstall(var NeedsRestart: Boolean): String;
var
  DataRoot: String;
begin
  Result := '';
  try
    DataRoot := SelectedDataRoot;
    if not IsValidDataRoot(DataRoot) then
      RaiseException('请选择与服务数据目录分开的安装目录。');
    if FileExists(ExpandConstant('{app}\native\Install-VLStream.ps1')) then
      Result := RunNative('Stop', ExistingDataRoot);
  except
    Result := GetExceptionMessage;
  end;
end;

procedure InitializeWizard;
begin
  ProvisioningPage := CreateOutputMarqueeProgressPage('正在安装 VLStream', '正在准备本机服务');
end;

procedure Provision(const DataRoot: String);
var
  ExitCode: Integer;
  Values: TArrayOfString;
  ErrorText: String;
begin
  if not HasVcRuntime then
  begin
    ProvisioningPage.SetText('正在安装 Microsoft Visual C++ x64 运行库', '如果 Windows 弹出授权窗口，请选择“是”。');
    if not ShellExec('runas', ExpandConstant('{app}\runtime\vc\vc_redist.x64.exe'),
      '/install /quiet /norestart', ExpandConstant('{app}'), SW_HIDE, ewWaitUntilTerminated, ExitCode) then
      RaiseException('无法启动 Microsoft Visual C++ x64 运行库安装程序。确认 Windows 授权后重试。' + #13#10 + SysErrorMessage(ExitCode));
    if (ExitCode = 3010) or (ExitCode = 1641) then
      RaiseException('Microsoft Visual C++ x64 运行库需要重启 Windows。重启后请再次运行 VLStream 安装程序。');
    if not HasVcRuntime then
      RaiseException('Microsoft Visual C++ x64 运行库未就绪。安装程序退出代码 ' + IntToStr(ExitCode) + '。');
  end;

  if not HasWebView2 then
  begin
    ProvisioningPage.SetText('正在安装 Microsoft Edge WebView2 运行时', '请保持网络连接。');
    if not Exec(ExpandConstant('{app}\runtime\WebView2\MicrosoftEdgeWebview2Setup.exe'),
      '/silent /install', ExpandConstant('{app}'), SW_HIDE, ewWaitUntilTerminated, ExitCode) then
      RaiseException('无法启动 Microsoft Edge WebView2 安装程序。' + #13#10 + SysErrorMessage(ExitCode));
    if not HasWebView2 then
      RaiseException('WebView2 未就绪。请检查网络后重试。安装程序退出代码 ' + IntToStr(ExitCode) + '。');
  end;

  ProvisioningPage.SetText('正在初始化并启动本机服务', '首次安装需要初始化数据库。请等待安装结果。');
  ErrorText := RunNative('Install', DataRoot);
  if ErrorText <> '' then
    RaiseException(ErrorText);
  SetArrayLength(Values, 1);
  Values[0] := DataRoot;
  if not SaveStringsToUTF8File(ExpandConstant('{app}\VLStreamDataRoot.txt'), Values, False) then
    RaiseException('无法保存 VLStream 服务数据目录。');
  DeleteFile(ExpandConstant('{group}\部署或启动 VLStream 服务.lnk'));
  DeleteFile(ExpandConstant('{group}\停止 VLStream 服务.lnk'));
end;

procedure CurStepChanged(CurStep: TSetupStep);
var
  DataRoot: String;
  ErrorText: String;
  ExitCode: Integer;
begin
  if (CurStep = ssDone) and ProvisioningSucceeded and not WizardSilent then
  begin
    if not Exec(ExpandConstant('{app}\VLStream.exe'), '', ExpandConstant('{app}'),
      SW_SHOWNORMAL, ewNoWait, ExitCode) then
      MsgBox('VLStream 已安装。请从开始菜单打开客户端。', mbInformation, MB_OK);
  end;
  if CurStep <> ssPostInstall then
    exit;

  DataRoot := SelectedDataRoot;
  repeat
    ErrorText := '';
    if not WizardSilent then
      ProvisioningPage.Show;
    try
      try
        Provision(DataRoot);
      except
        ErrorText := GetExceptionMessage;
      end;
    finally
      if not WizardSilent then
        ProvisioningPage.Hide;
    end;
    if ErrorText = '' then
    begin
      ProvisioningSucceeded := True;
      exit;
    end;
    ErrorText := ErrorText + #13#10#13#10 + '安装日志 ' + ExpandConstant('{log}') + #13#10 +
      '服务日志目录 ' + AddBackslash(DataRoot) + 'logs';
    Log(ErrorText);
    if WizardSilent then
      exit;
  until MsgBox(ErrorText, mbError, MB_RETRYCANCEL) <> IDRETRY;
end;

procedure CurPageChanged(CurPageID: Integer);
begin
  if (CurPageID = wpFinished) and not ProvisioningSucceeded then
  begin
    WizardForm.FinishedHeadingLabel.Caption := 'VLStream 安装未完成';
    WizardForm.FinishedLabel.Caption := '本机服务尚未就绪。请检查安装日志，解决问题后重新运行安装程序。';
  end;
end;

function GetCustomSetupExitCode: Integer;
begin
  if ProvisioningSucceeded then
    Result := 0
  else
    Result := 20;
end;

procedure CurUninstallStepChanged(CurUninstallStep: TUninstallStep);
var
  ErrorText: String;
begin
  if (CurUninstallStep = usUninstall) and FileExists(ExpandConstant('{app}\native\Install-VLStream.ps1')) then
  begin
    ErrorText := RunNative('Stop', ExistingDataRoot);
    if ErrorText <> '' then
    begin
      Log(ErrorText);
      if not UninstallSilent then
        MsgBox('无法停止本机服务。请重试卸载。' + #13#10 + ErrorText, mbError, MB_OK);
      Abort;
    end;
  end;
end;
