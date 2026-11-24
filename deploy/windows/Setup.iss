#ifndef DeliveryRoot
  #error Pass the absolute delivery directory with /DDeliveryRoot.
#endif

[Setup]
AppId={{0A15FD16-A008-4816-BCFE-1451EA6606EB}
AppName=VLStream
AppVersion=1.2.12
AppPublisher=OortCloudGroup
DefaultDirName={localappdata}\Programs\VLStream
DefaultGroupName=VLStream
DisableProgramGroupPage=yes
PrivilegesRequired=lowest
MinVersion=10.0.19045
ArchitecturesAllowed=x64os
ArchitecturesInstallIn64BitMode=x64os
UninstallDisplayIcon={app}\VLStream.exe
OutputDir={#DeliveryRoot}
OutputBaseFilename=VLStream-Cloud-v1.2.12-Windows-x64-Setup
Compression=lzma2
SolidCompression=yes
WizardStyle=modern

[Tasks]
Name: "desktopicon"; Description: "创建 VLStream 桌面快捷方式"

[Files]
Source: "{#DeliveryRoot}\verify\client\*"; DestDir: "{app}"; Flags: ignoreversion
Source: "server\*"; DestDir: "{app}\server"; Flags: ignoreversion recursesubdirs
Source: "Start-VLStreamServer.cmd"; DestDir: "{app}"; Flags: ignoreversion
Source: "{#DeliveryRoot}\release\*.zip*"; DestDir: "{app}\release"; Flags: ignoreversion
Source: "{#DeliveryRoot}\database\*"; DestDir: "{app}\database"; Flags: ignoreversion recursesubdirs
Source: "{#DeliveryRoot}\Windows安装说明.md"; DestDir: "{app}"; Flags: ignoreversion

[Icons]
Name: "{group}\VLStream"; Filename: "{app}\VLStream.exe"; WorkingDir: "{app}"
Name: "{group}\部署或启动 VLStream 服务"; Filename: "{app}\Start-VLStreamServer.cmd"; WorkingDir: "{app}"
Name: "{group}\停止 VLStream 服务"; Filename: "{app}\Start-VLStreamServer.cmd"; Parameters: "stop"; WorkingDir: "{app}"
Name: "{group}\VLStream 安装说明"; Filename: "{sys}\notepad.exe"; Parameters: """{app}\Windows安装说明.md"""; WorkingDir: "{app}"
Name: "{userdesktop}\VLStream"; Filename: "{app}\VLStream.exe"; WorkingDir: "{app}"; Tasks: desktopicon

[Run]
Filename: "{cmd}"; Parameters: "/d /c """"{app}\Start-VLStreamServer.cmd"""""; WorkingDir: "{app}"; Description: "部署并启动本机 VLStream 服务"; Flags: nowait postinstall skipifsilent
