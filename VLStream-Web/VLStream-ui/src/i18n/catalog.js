const localeColumns = ['zh-CN', 'en-US', 'es-MX', 'ar', 'de-DE', 'fr-FR', 'ja-JP', 'pt-BR', 'ru-RU', 'ko-KR', 'id-ID', 'tr-TR']

export const localeOptions = [
  { code: 'zh-CN', name: '简体中文', shortName: '中文', dir: 'ltr' },
  { code: 'en-US', name: 'English (US)', shortName: 'English', dir: 'ltr' },
  { code: 'es-MX', name: 'Español (Latinoamérica)', shortName: 'Español', dir: 'ltr' },
  { code: 'ar', name: 'العربية', shortName: 'العربية', dir: 'rtl' },
  { code: 'de-DE', name: 'Deutsch', shortName: 'Deutsch', dir: 'ltr' },
  { code: 'fr-FR', name: 'Français', shortName: 'Français', dir: 'ltr' },
  { code: 'ja-JP', name: '日本語', shortName: '日本語', dir: 'ltr' },
  { code: 'pt-BR', name: 'Português (Brasil)', shortName: 'Português', dir: 'ltr' },
  { code: 'ru-RU', name: 'Русский', shortName: 'Русский', dir: 'ltr' },
  { code: 'ko-KR', name: '한국어', shortName: '한국어', dir: 'ltr' },
  { code: 'id-ID', name: 'Bahasa Indonesia', shortName: 'Indonesia', dir: 'ltr' },
  { code: 'tr-TR', name: 'Türkçe', shortName: 'Türkçe', dir: 'ltr' }
]

// This catalog is also consumed by the legacy DOM compatibility layer. Keep the
// Chinese source text in the first column so existing views can be localized
// without changing their business logic.
const catalogTsv = `
登录\tLogin\tIniciar sesión\tتسجيل الدخول\tAnmelden\tConnexion\tログイン\tEntrar\tВойти\t로그인\tMasuk\tGiriş yap
工作台\tWorkspace\tÁrea de trabajo\tمساحة العمل\tArbeitsbereich\tEspace de travail\tワークスペース\tÁrea de trabalho\tРабочая область\t작업 공간\tRuang kerja\tÇalışma alanı
视频广场\tVideo wall\tMosaico de video\tجدار الفيديو\tVideowand\tMur vidéo\tビデオウォール\tPainel de vídeo\tВидеостена\t비디오 월\tDinding video\tVideo duvarı
设备管理\tDevice management\tAdministración de dispositivos\tإدارة الأجهزة\tGeräteverwaltung\tGestion des appareils\tデバイス管理\tGerenciamento de dispositivos\tУправление устройствами\t장치 관리\tManajemen perangkat\tCihaz yönetimi
VLStream 设备\tVLStream devices\tDispositivos VLStream\tأجهزة VLStream\tVLStream-Geräte\tAppareils VLStream\tVLStream デバイス\tDispositivos VLStream\tУстройства VLStream\tVLStream 장치\tPerangkat VLStream\tVLStream cihazları
自定义协议设备\tCustom protocol devices\tDispositivos de protocolo personalizado\tأجهزة البروتوكول المخصص\tGeräte mit benutzerdefiniertem Protokoll\tAppareils à protocole personnalisé\tカスタムプロトコルデバイス\tDispositivos de protocolo personalizado\tУстройства с пользовательским протоколом\t사용자 지정 프로토콜 장치\tPerangkat protokol khusus\tÖzel protokol cihazları
国标设备\tGB28181 devices\tDispositivos GB28181\tأجهزة GB28181\tGB28181-Geräte\tAppareils GB28181\tGB28181 デバイス\tDispositivos GB28181\tУстройства GB28181\tGB28181 장치\tPerangkat GB28181\tGB28181 cihazları
云端录像\tCloud recordings\tGrabaciones en la nube\tالتسجيلات السحابية\tCloud-Aufzeichnungen\tEnregistrements cloud\tクラウド録画\tGravações na nuvem\tОблачные записи\t클라우드 녹화\tRekaman cloud\tBulut kayıtları
推流列表\tStream publishing\tPublicación de transmisiones\tنشر البث\tStream-Veröffentlichung\tPublication de flux\tストリーム配信\tPublicação de streams\tПубликация потоков\t스트림 게시\tPublikasi stream\tAkış yayınlama
拉流列表\tStream proxies\tProxies de transmisión\tوكلاء البث\tStream-Proxys\tProxys de flux\tストリームプロキシ\tProxies de stream\tПрокси потоков\t스트림 프록시\tProksi stream\tAkış proxy'leri
国标级联\tGB28181 cascading\tCascada GB28181\tتسلسل GB28181\tGB28181-Kaskadierung\tCascade GB28181\tGB28181 カスケード\tCascata GB28181\tКаскад GB28181\tGB28181 계단식 연결\tKaskade GB28181\tGB28181 kademelendirme
录像计划\tRecording plans\tPlanes de grabación\tخطط التسجيل\tAufnahmepläne\tPlans d’enregistrement\t録画プラン\tPlanos de gravação\tПланы записи\t녹화 계획\tRencana perekaman\tKayıt planları
行政分组\tAdministrative groups\tGrupos administrativos\tالمجموعات الإدارية\tVerwaltungsgruppen\tGroupes administratifs\t管理グループ\tGrupos administrativos\tАдминистративные группы\t관리 그룹\tGrup administratif\tİdari gruplar
通道管理\tChannel management\tAdministración de canales\tإدارة القنوات\tKanalverwaltung\tGestion des canaux\tチャンネル管理\tGerenciamento de canais\tУправление каналами\t채널 관리\tManajemen kanal\tKanal yönetimi
报警管理\tAlarm management\tAdministración de alarmas\tإدارة الإنذارات\tAlarmverwaltung\tGestion des alarmes\tアラーム管理\tGerenciamento de alarmes\tУправление тревогами\t알람 관리\tManajemen alarm\tAlarm yönetimi
节点管理\tNode management\tAdministración de nodos\tإدارة العقد\tKnotenverwaltung\tGestion des nœuds\tノード管理\tGerenciamento de nós\tУправление узлами\t노드 관리\tManajemen node\tDüğüm yönetimi
分屏监控\tSplit-screen monitoring\tMonitoreo multipantalla\tالمراقبة متعددة الشاشات\tMehrbildüberwachung\tSurveillance multi-écrans\t分割画面監視\tMonitoramento em tela dividida\tМногоэкранный мониторинг\t분할 화면 모니터링\tPemantauan layar terbagi\tBölünmüş ekran izleme
通道列表\tChannel list\tLista de canales\tقائمة القنوات\tKanalliste\tListe des canaux\tチャンネル一覧\tLista de canais\tСписок каналов\t채널 목록\tDaftar kanal\tKanal listesi
设备录像\tDevice recordings\tGrabaciones del dispositivo\tتسجيلات الجهاز\tGeräteaufzeichnungen\tEnregistrements de l’appareil\tデバイス録画\tGravações do dispositivo\tЗаписи устройства\t장치 녹화\tRekaman perangkat\tCihaz kayıtları
通道共享\tChannel sharing\tUso compartido de canales\tمشاركة القنوات\tKanalfreigabe\tPartage de canaux\tチャンネル共有\tCompartilhamento de canais\tОбщий доступ к каналам\t채널 공유\tBerbagi kanal\tKanal paylaşımı
关联通道\tLinked channels\tCanales vinculados\tالقنوات المرتبطة\tVerknüpfte Kanäle\tCanaux associés\t関連チャンネル\tCanais vinculados\tСвязанные каналы\t연결된 채널\tKanal tertaut\tBağlı kanallar
新增节点\tAdd node\tAgregar nodo\tإضافة عقدة\tKnoten hinzufügen\tAjouter un nœud\tノードを追加\tAdicionar nó\tДобавить узел\t노드 추가\tTambah node\tDüğüm ekle
编辑节点\tEdit node\tEditar nodo\tتحرير العقدة\tKnoten bearbeiten\tModifier le nœud\tノードを編集\tEditar nó\tИзменить узел\t노드 편집\tEdit node\tDüğümü düzenle
编辑设备\tEdit device\tEditar dispositivo\tتحرير الجهاز\tGerät bearbeiten\tModifier l’appareil\tデバイスを編集\tEditar dispositivo\tИзменить устройство\t장치 편집\tEdit perangkat\tCihazı düzenle
配置参数\tConfiguration parameters\tParámetros de configuración\tمعلمات التكوين\tKonfigurationsparameter\tParamètres de configuration\t構成パラメーター\tParâmetros de configuração\tПараметры конфигурации\t구성 매개변수\tParameter konfigurasi\tYapılandırma parametreleri
AI事件配置\tAI event configuration\tConfiguración de eventos de IA\tتكوين أحداث الذكاء الاصطناعي\tKI-Ereigniskonfiguration\tConfiguration des événements IA\tAI イベント設定\tConfiguração de eventos de IA\tНастройка событий ИИ\tAI 이벤트 구성\tKonfigurasi peristiwa AI\tYapay zekâ olay yapılandırması
设置摄像机\tCamera settings\tConfiguración de la cámara\tإعدادات الكاميرا\tKameraeinstellungen\tParamètres de la caméra\tカメラ設定\tConfigurações da câmera\tНастройки камеры\t카메라 설정\tPengaturan kamera\tKamera ayarları
事件管理\tEvent management\tAdministración de eventos\tإدارة الأحداث\tEreignisverwaltung\tGestion des événements\tイベント管理\tGerenciamento de eventos\tУправление событиями\t이벤트 관리\tManajemen peristiwa\tOlay yönetimi
算法管理\tAlgorithm management\tAdministración de algoritmos\tإدارة الخوارزميات\tAlgorithmenverwaltung\tGestion des algorithmes\tアルゴリズム管理\tGerenciamento de algoritmos\tУправление алгоритмами\t알고리즘 관리\tManajemen algoritme\tAlgoritma yönetimi
大模型管理\tLLM management\tAdministración de modelos de lenguaje\tإدارة النماذج اللغوية\tLLM-Verwaltung\tGestion des grands modèles\tLLM 管理\tGerenciamento de LLM\tУправление языковыми моделями\tLLM 관리\tManajemen LLM\tLLM yönetimi
大模型复核\tLLM review\tRevisión con modelos de lenguaje\tالمراجعة بالنماذج اللغوية\tLLM-Prüfung\tRévision par grand modèle\tLLM レビュー\tRevisão por LLM\tПроверка языковой моделью\tLLM 검토\tTinjauan LLM\tLLM incelemesi
场景治理\tScene governance\tGobernanza de escenarios\tحوكمة السيناريوهات\tSzenarioverwaltung\tGouvernance des scénarios\tシーンガバナンス\tGovernança de cenários\tУправление сценариями\t시나리오 거버넌스\tTata kelola skenario\tSenaryo yönetişimi
智能分析申请\tIntelligent analysis requests\tSolicitudes de análisis inteligente\tطلبات التحليل الذكي\tAnträge auf intelligente Analyse\tDemandes d’analyse intelligente\tインテリジェント分析申請\tSolicitações de análise inteligente\tЗапросы интеллектуального анализа\t지능형 분석 요청\tPermintaan analisis cerdas\tAkıllı analiz istekleri
智能分析结果\tIntelligent analysis results\tResultados del análisis inteligente\tنتائج التحليل الذكي\tErgebnisse der intelligenten Analyse\tRésultats de l’analyse intelligente\tインテリジェント分析結果\tResultados da análise inteligente\tРезультаты интеллектуального анализа\t지능형 분석 결과\tHasil analisis cerdas\tAkıllı analiz sonuçları
算法训练平台\tAlgorithm training platform\tPlataforma de entrenamiento de algoritmos\tمنصة تدريب الخوارزميات\tPlattform für Algorithmustraining\tPlateforme d’entraînement des algorithmes\tアルゴリズム学習プラットフォーム\tPlataforma de treinamento de algoritmos\tПлатформа обучения алгоритмов\t알고리즘 학습 플랫폼\tPlatform pelatihan algoritme\tAlgoritma eğitim platformu
数据集管理\tDataset management\tAdministración de conjuntos de datos\tإدارة مجموعات البيانات\tDatensatzverwaltung\tGestion des jeux de données\tデータセット管理\tGerenciamento de conjuntos de dados\tУправление наборами данных\t데이터 세트 관리\tManajemen set data\tVeri kümesi yönetimi
算法标注\tAlgorithm annotation\tAnotación de algoritmos\tوسم الخوارزميات\tAlgorithmenannotation\tAnnotation d’algorithmes\tアルゴリズムアノテーション\tAnotação de algoritmos\tРазметка алгоритмов\t알고리즘 주석\tAnotasi algoritme\tAlgoritma etiketleme
算法模型\tAlgorithm models\tModelos de algoritmos\tنماذج الخوارزميات\tAlgorithmenmodelle\tModèles d’algorithmes\tアルゴリズムモデル\tModelos de algoritmos\tМодели алгоритмов\t알고리즘 모델\tModel algoritme\tAlgoritma modelleri
算法训练\tAlgorithm training\tEntrenamiento de algoritmos\tتدريب الخوارزميات\tAlgorithmustraining\tEntraînement des algorithmes\tアルゴリズム学習\tTreinamento de algoritmos\tОбучение алгоритмов\t알고리즘 학습\tPelatihan algoritme\tAlgoritma eğitimi
SSH连接\tSSH connections\tConexiones SSH\tاتصالات SSH\tSSH-Verbindungen\tConnexions SSH\tSSH 接続\tConexões SSH\tSSH-подключения\tSSH 연결\tKoneksi SSH\tSSH bağlantıları
监控告警\tMonitoring and alarms\tMonitoreo y alarmas\tالمراقبة والإنذارات\tÜberwachung und Alarme\tSurveillance et alarmes\t監視とアラーム\tMonitoramento e alarmes\tМониторинг и тревоги\t모니터링 및 알람\tPemantauan dan alarm\tİzleme ve alarmlar
视频回放\tVideo playback\tReproducción de video\tتشغيل الفيديو\tVideowiedergabe\tLecture vidéo\tビデオ再生\tReprodução de vídeo\tВоспроизведение видео\t비디오 재생\tPemutaran video\tVideo oynatma
设计图\tDesign drawing\tPlano de diseño\tمخطط التصميم\tEntwurfszeichnung\tPlan de conception\t設計図\tDesenho do projeto\tПроектный чертёж\t설계 도면\tGambar desain\tTasarım çizimi
编辑场景\tEdit scene\tEditar escenario\tتحرير السيناريو\tSzenario bearbeiten\tModifier le scénario\tシーンを編集\tEditar cenário\tИзменить сценарий\t시나리오 편집\tEdit skenario\tSenaryoyu düzenle
算法配置\tAlgorithm configuration\tConfiguración de algoritmos\tتكوين الخوارزميات\tAlgorithmenkonfiguration\tConfiguration des algorithmes\tアルゴリズム設定\tConfiguração de algoritmos\tНастройка алгоритмов\t알고리즘 구성\tKonfigurasi algoritme\tAlgoritma yapılandırması
任务配置\tTask configuration\tConfiguración de tareas\tتكوين المهام\tAufgabenkonfiguration\tConfiguration des tâches\tタスク設定\tConfiguração de tarefas\tНастройка задач\t작업 구성\tKonfigurasi tugas\tGörev yapılandırması
容器实例\tContainer instances\tInstancias de contenedor\tمثيلات الحاويات\tContainer-Instanzen\tInstances de conteneur\tコンテナインスタンス\tInstâncias de contêiner\tЭкземпляры контейнеров\t컨테이너 인스턴스\tInstans kontainer\tKapsayıcı örnekleri
个人设置\tPersonal settings\tConfiguración personal\tالإعدادات الشخصية\tPersönliche Einstellungen\tParamètres personnels\t個人設定\tConfigurações pessoais\tЛичные настройки\t개인 설정\tPengaturan pribadi\tKişisel ayarlar
云平台\tCloud platform\tPlataforma en la nube\tالمنصة السحابية\tCloud-Plattform\tPlateforme cloud\tクラウドプラットフォーム\tPlataforma em nuvem\tОблачная платформа\t클라우드 플랫폼\tPlatform cloud\tBulut platformu
用户管理\tUser management\tAdministración de usuarios\tإدارة المستخدمين\tBenutzerverwaltung\tGestion des utilisateurs\tユーザー管理\tGerenciamento de usuários\tУправление пользователями\t사용자 관리\tManajemen pengguna\tKullanıcı yönetimi
角色管理\tRole management\tAdministración de roles\tإدارة الأدوار\tRollenverwaltung\tGestion des rôles\tロール管理\tGerenciamento de funções\tУправление ролями\t역할 관리\tManajemen peran\tRol yönetimi
菜单管理\tMenu management\tAdministración de menús\tإدارة القوائم\tMenüverwaltung\tGestion des menus\tメニュー管理\tGerenciamento de menus\tУправление меню\t메뉴 관리\tManajemen menu\tMenü yönetimi
部门管理\tDepartment management\tAdministración de departamentos\tإدارة الأقسام\tAbteilungsverwaltung\tGestion des services\t部門管理\tGerenciamento de departamentos\tУправление подразделениями\t부서 관리\tManajemen departemen\tDepartman yönetimi
岗位管理\tPosition management\tAdministración de puestos\tإدارة المناصب\tStellenverwaltung\tGestion des postes\t役職管理\tGerenciamento de cargos\tУправление должностями\t직책 관리\tManajemen jabatan\tPozisyon yönetimi
数据权限\tData permissions\tPermisos de datos\tأذونات البيانات\tDatenberechtigungen\tAutorisations de données\tデータ権限\tPermissões de dados\tПрава на данные\t데이터 권한\tIzin data\tVeri izinleri
接口权限\tAPI permissions\tPermisos de API\tأذونات واجهة API\tAPI-Berechtigungen\tAutorisations d’API\tAPI 権限\tPermissões de API\tПрава API\tAPI 권한\tIzin API\tAPI izinleri
固件管理\tFirmware management\tAdministración de firmware\tإدارة البرامج الثابتة\tFirmwareverwaltung\tGestion des micrologiciels\tファームウェア管理\tGerenciamento de firmware\tУправление прошивками\t펌웨어 관리\tManajemen firmware\tÜrün yazılımı yönetimi
主动安全事件\tActive safety events\tEventos de seguridad activa\tأحداث السلامة النشطة\tAktive Sicherheitsereignisse\tÉvénements de sécurité active\tアクティブセーフティイベント\tEventos de segurança ativa\tСобытия активной безопасности\t능동 안전 이벤트\tPeristiwa keselamatan aktif\tAktif güvenlik olayları
我的工单\tMy work orders\tMis órdenes de trabajo\tأوامر العمل الخاصة بي\tMeine Arbeitsaufträge\tMes ordres de travail\t自分の作業指示\tMinhas ordens de serviço\tМои наряды\t내 작업 지시\tPerintah kerja saya\tİş emirlerim
待办工单\tPending work orders\tÓrdenes de trabajo pendientes\tأوامر العمل المعلقة\tAusstehende Arbeitsaufträge\tOrdres de travail en attente\t保留中の作業指示\tOrdens de serviço pendentes\tОжидающие наряды\t대기 중인 작업 지시\tPerintah kerja tertunda\tBekleyen iş emirleri
已办工单\tCompleted work orders\tÓrdenes de trabajo completadas\tأوامر العمل المكتملة\tAbgeschlossene Arbeitsaufträge\tOrdres de travail terminés\t完了した作業指示\tOrdens de serviço concluídas\tВыполненные наряды\t완료된 작업 지시\tPerintah kerja selesai\tTamamlanan iş emirleri
可接工单\tAvailable work orders\tÓrdenes de trabajo disponibles\tأوامر العمل المتاحة\tVerfügbare Arbeitsaufträge\tOrdres de travail disponibles\t受注可能な作業指示\tOrdens de serviço disponíveis\tДоступные наряды\t수락 가능한 작업 지시\tPerintah kerja tersedia\tAlınabilir iş emirleri
主动安全设置\tActive safety settings\tConfiguración de seguridad activa\tإعدادات السلامة النشطة\tAktive Sicherheitseinstellungen\tParamètres de sécurité active\tアクティブセーフティ設定\tConfigurações de segurança ativa\tНастройки активной безопасности\t능동 안전 설정\tPengaturan keselamatan aktif\tAktif güvenlik ayarları
工单设置\tWork order settings\tConfiguración de órdenes de trabajo\tإعدادات أوامر العمل\tArbeitsauftragseinstellungen\tParamètres des ordres de travail\t作業指示設定\tConfigurações de ordens de serviço\tНастройки нарядов\t작업 지시 설정\tPengaturan perintah kerja\tİş emri ayarları
工单详情\tWork order details\tDetalles de la orden de trabajo\tتفاصيل أمر العمل\tArbeitsauftragsdetails\tDétails de l’ordre de travail\t作業指示の詳細\tDetalhes da ordem de serviço\tСведения о наряде\t작업 지시 세부 정보\tDetail perintah kerja\tİş emri ayrıntıları
欢迎您\tWelcome\tBienvenido\tمرحبًا\tWillkommen\tBienvenue\tようこそ\tBoas-vindas\tДобро пожаловать\t환영합니다\tSelamat datang\tHoş geldiniz
全屏\tFullscreen\tPantalla completa\tملء الشاشة\tVollbild\tPlein écran\t全画面\tTela cheia\tПолный экран\t전체 화면\tLayar penuh\tTam ekran
退出全屏\tExit fullscreen\tSalir de pantalla completa\tالخروج من ملء الشاشة\tVollbild beenden\tQuitter le plein écran\t全画面を終了\tSair da tela cheia\tВыйти из полноэкранного режима\t전체 화면 종료\tKeluar dari layar penuh\tTam ekrandan çık
消息中心\tMessage center\tCentro de mensajes\tمركز الرسائل\tNachrichtenzentrale\tCentre de messages\tメッセージセンター\tCentral de mensagens\tЦентр сообщений\t메시지 센터\tPusat pesan\tMesaj merkezi
已实名\tVerified\tIdentidad verificada\tتم التحقق\tVerifiziert\tIdentité vérifiée\t本人確認済み\tIdentidade verificada\tЛичность подтверждена\t본인 인증됨\tTerverifikasi\tDoğrulandı
未实名\tUnverified\tSin verificar\tغير موثق\tNicht verifiziert\tNon vérifié\t未確認\tNão verificado\tНе подтверждено\t미인증\tBelum terverifikasi\tDoğrulanmadı
超级管理员\tSuper administrator\tSuperadministrador\tالمشرف العام\tSuperadministrator\tSuper administrateur\tスーパー管理者\tSuperadministrador\tСуперадминистратор\t최고 관리자\tSuper administrator\tSüper yönetici
租户管理员\tTenant administrator\tAdministrador del inquilino\tمسؤول المستأجر\tMandantenadministrator\tAdministrateur du locataire\tテナント管理者\tAdministrador do locatário\tАдминистратор арендатора\t테넌트 관리자\tAdministrator tenant\tKiracı yöneticisi
切换账号\tSwitch account\tCambiar cuenta\tتبديل الحساب\tKonto wechseln\tChanger de compte\tアカウントを切り替え\tTrocar conta\tСменить аккаунт\t계정 전환\tGanti akun\tHesap değiştir
退出登录\tSign out\tCerrar sesión\tتسجيل الخروج\tAbmelden\tDéconnexion\tログアウト\tSair\tВыйти\t로그아웃\tKeluar\tÇıkış yap
应用\tApps\tAplicaciones\tالتطبيقات\tApps\tApplications\tアプリ\tAplicativos\tПриложения\t앱\tAplikasi\tUygulamalar
更多应用\tMore apps\tMás aplicaciones\tالمزيد من التطبيقات\tWeitere Apps\tPlus d’applications\tその他のアプリ\tMais aplicativos\tДругие приложения\t더 많은 앱\tAplikasi lainnya\tDaha fazla uygulama
当前租户\tCurrent tenant\tInquilino actual\tالمستأجر الحالي\tAktueller Mandant\tLocataire actuel\t現在のテナント\tLocatário atual\tТекущий арендатор\t현재 테넌트\tTenant saat ini\tGeçerli kiracı
主题\tTheme\tTema\tالسمة\tDesign\tThème\tテーマ\tTema\tТема\t테마\tTema\tTema
关闭\tClose\tCerrar\tإغلاق\tSchließen\tFermer\t閉じる\tFechar\tЗакрыть\t닫기\tTutup\tKapat
未读\tUnread\tNo leídos\tغير مقروء\tUngelesen\tNon lus\t未読\tNão lidas\tНепрочитанные\t읽지 않음\tBelum dibaca\tOkunmamış
已读\tRead\tLeídos\tمقروء\tGelesen\tLus\t既読\tLidas\tПрочитанные\t읽음\tSudah dibaca\tOkunmuş
暂无数据\tNo data\tSin datos\tلا توجد بيانات\tKeine Daten\tAucune donnée\tデータなし\tSem dados\tНет данных\t데이터 없음\tTidak ada data\tVeri yok
提示\tNotice\tAviso\tتنبيه\tHinweis\tAvis\tお知らせ\tAviso\tУведомление\t알림\tPemberitahuan\tBildirim
确定\tConfirm\tConfirmar\tتأكيد\tBestätigen\tConfirmer\t確認\tConfirmar\tПодтвердить\t확인\tKonfirmasi\tOnayla
取消\tCancel\tCancelar\tإلغاء\tAbbrechen\tAnnuler\tキャンセル\tCancelar\tОтмена\t취소\tBatal\tİptal
保存\tSave\tGuardar\tحفظ\tSpeichern\tEnregistrer\t保存\tSalvar\tСохранить\t저장\tSimpan\tKaydet
新增\tAdd\tAgregar\tإضافة\tHinzufügen\tAjouter\t追加\tAdicionar\tДобавить\t추가\tTambah\tEkle
编辑\tEdit\tEditar\tتحرير\tBearbeiten\tModifier\t編集\tEditar\tИзменить\t편집\tEdit\tDüzenle
删除\tDelete\tEliminar\tحذف\tLöschen\tSupprimer\t削除\tExcluir\tУдалить\t삭제\tHapus\tSil
查询\tSearch\tBuscar\tبحث\tSuchen\tRechercher\t検索\tPesquisar\tПоиск\t검색\tCari\tAra
重置\tReset\tRestablecer\tإعادة تعيين\tZurücksetzen\tRéinitialiser\tリセット\tRedefinir\tСбросить\t재설정\tAtur ulang\tSıfırla
操作\tActions\tAcciones\tالإجراءات\tAktionen\tActions\t操作\tAções\tДействия\t작업\tTindakan\tİşlemler
状态\tStatus\tEstado\tالحالة\tStatus\tÉtat\tステータス\tStatus\tСтатус\t상태\tStatus\tDurum
成功\tSuccess\tÉxito\tنجاح\tErfolgreich\tRéussite\t成功\tSucesso\tУспешно\t성공\tBerhasil\tBaşarılı
失败\tFailed\tError\tفشل\tFehlgeschlagen\tÉchec\t失敗\tFalha\tОшибка\t실패\tGagal\tBaşarısız
加载中\tLoading\tCargando\tجارٍ التحميل\tWird geladen\tChargement\t読み込み中\tCarregando\tЗагрузка\t로딩 중\tMemuat\tYükleniyor
在线\tOnline\tEn línea\tمتصل\tOnline\tEn ligne\tオンライン\tOnline\tВ сети\t온라인\tOnline\tÇevrimiçi
离线\tOffline\tSin conexión\tغير متصل\tOffline\tHors ligne\tオフライン\tOffline\tНе в сети\t오프라인\tOffline\tÇevrimdışı
更多\tMore\tMás\tالمزيد\tMehr\tPlus\tその他\tMais\tЕщё\t더보기\tLainnya\tDaha fazla
语言\tLanguage\tIdioma\tاللغة\tSprache\tLangue\t言語\tIdioma\tЯзык\t언어\tBahasa\tDil
请输入用户名\tEnter your username\tIngresa tu nombre de usuario\tأدخل اسم المستخدم\tBenutzernamen eingeben\tSaisissez votre nom d’utilisateur\tユーザー名を入力してください\tDigite seu nome de usuário\tВведите имя пользователя\t사용자 이름을 입력하세요\tMasukkan nama pengguna\tKullanıcı adınızı girin
请输入密码\tEnter your password\tIngresa tu contraseña\tأدخل كلمة المرور\tPasswort eingeben\tSaisissez votre mot de passe\tパスワードを入力してください\tDigite sua senha\tВведите пароль\t비밀번호를 입력하세요\tMasukkan kata sandi\tParolanızı girin
登录中...\tSigning in...\tIniciando sesión...\tجارٍ تسجيل الدخول...\tAnmeldung läuft...\tConnexion en cours...\tログイン中...\tEntrando...\tВход...\t로그인 중...\tSedang masuk...\tGiriş yapılıyor...
请从应用平台进入\tOpen VLStream from the application platform\tAbre VLStream desde la plataforma de aplicaciones\tافتح VLStream من منصة التطبيقات\tVLStream über die Anwendungsplattform öffnen\tOuvrez VLStream depuis la plateforme d’applications\tアプリケーションプラットフォームから VLStream を開いてください\tAbra o VLStream pela plataforma de aplicativos\tОткройте VLStream с платформы приложений\t애플리케이션 플랫폼에서 VLStream을 여세요\tBuka VLStream dari platform aplikasi\tVLStream'i uygulama platformundan açın
当前服务已启用多租户模式，请从应用平台点击 VLStream 进入。\tMulti-tenant mode is enabled. Open VLStream from the application platform.\tEl modo multiinquilino está habilitado. Abre VLStream desde la plataforma de aplicaciones.\tتم تمكين وضع تعدد المستأجرين. افتح VLStream من منصة التطبيقات.\tDer Mehrmandantenmodus ist aktiviert. Öffnen Sie VLStream über die Anwendungsplattform.\tLe mode multilocataire est activé. Ouvrez VLStream depuis la plateforme d’applications.\tマルチテナントモードが有効です。アプリケーションプラットフォームから VLStream を開いてください。\tO modo multilocatário está ativado. Abra o VLStream pela plataforma de aplicativos.\tВключён мультитенантный режим. Откройте VLStream с платформы приложений.\t멀티테넌트 모드가 활성화되었습니다. 애플리케이션 플랫폼에서 VLStream을 여세요.\tMode multi-tenant aktif. Buka VLStream dari platform aplikasi.\tÇok kiracılı mod etkin. VLStream'i uygulama platformundan açın.
前往应用平台\tGo to application platform\tIr a la plataforma de aplicaciones\tالانتقال إلى منصة التطبيقات\tZur Anwendungsplattform\tAccéder à la plateforme d’applications\tアプリケーションプラットフォームへ\tIr para a plataforma de aplicativos\tПерейти на платформу приложений\t애플리케이션 플랫폼으로 이동\tBuka platform aplikasi\tUygulama platformuna git
正在读取登录模式...\tLoading sign-in mode...\tCargando el modo de inicio de sesión...\tجارٍ تحميل وضع تسجيل الدخول...\tAnmeldemodus wird geladen...\tChargement du mode de connexion...\tログインモードを読み込み中...\tCarregando o modo de entrada...\tЗагрузка режима входа...\t로그인 모드를 불러오는 중...\tMemuat mode masuk...\tGiriş modu yükleniyor...
密码长度至少6位\tPassword must be at least 6 characters\tLa contraseña debe tener al menos 6 caracteres\tيجب ألا تقل كلمة المرور عن 6 أحرف\tDas Passwort muss mindestens 6 Zeichen lang sein\tLe mot de passe doit contenir au moins 6 caractères\tパスワードは6文字以上で入力してください\tA senha deve ter pelo menos 6 caracteres\tПароль должен содержать не менее 6 символов\t비밀번호는 6자 이상이어야 합니다\tKata sandi minimal 6 karakter\tParola en az 6 karakter olmalıdır
登录成功\tSigned in successfully\tInicio de sesión correcto\tتم تسجيل الدخول بنجاح\tAnmeldung erfolgreich\tConnexion réussie\tログインしました\tLogin realizado com sucesso\tВход выполнен\t로그인했습니다\tBerhasil masuk\tGiriş başarılı
登录失败\tSign-in failed\tError al iniciar sesión\tفشل تسجيل الدخول\tAnmeldung fehlgeschlagen\tÉchec de la connexion\tログインに失敗しました\tFalha ao entrar\tНе удалось войти\t로그인 실패\tGagal masuk\tGiriş başarısız
切换语言\tSwitch language\tCambiar idioma\tتغيير اللغة\tSprache wechseln\tChanger de langue\t言語を切り替える\tAlterar idioma\tСменить язык\t언어 변경\tGanti bahasa\tDili değiştir
视频汇聚\tVideo aggregation\tAgregación de video\tتجميع الفيديو\tVideoaggregation\tAgrégation vidéo\tビデオ統合\tAgregação de vídeo\tАгрегация видео\t비디오 통합\tAgregasi video\tVideo toplama
决策式AI\tDecision AI\tIA para decisiones\tذكاء اصطناعي للقرارات\tEntscheidungs-KI\tIA décisionnelle\t意思決定 AI\tIA de decisão\tИИ для принятия решений\t의사결정 AI\tAI keputusan\tKarar yapay zekâsı
算法仓库\tAlgorithm repository\tRepositorio de algoritmos\tمستودع الخوارزميات\tAlgorithmen-Repository\tRéférentiel d’algorithmes\tアルゴリズムリポジトリ\tRepositório de algoritmos\tРепозиторий алгоритмов\t알고리즘 저장소\tRepositori algoritme\tAlgoritma deposu
AI算力调度\tAI compute scheduling\tProgramación de cómputo de IA\tجدولة حوسبة الذكاء الاصطناعي\tKI-Rechenplanung\tPlanification des ressources IA\tAI コンピューティングスケジューリング\tAgendamento de computação de IA\tПланирование вычислений ИИ\tAI 컴퓨팅 스케줄링\tPenjadwalan komputasi AI\tYapay zekâ işlem planlama
系统管理\tSystem management\tAdministración del sistema\tإدارة النظام\tSystemverwaltung\tAdministration système\tシステム管理\tGerenciamento do sistema\tУправление системой\t시스템 관리\tManajemen sistem\tSistem yönetimi
待审批\tPending approval\tPendientes de aprobación\tبانتظار الموافقة\tAusstehende Genehmigungen\tEn attente d’approbation\t承認待ち\tAguardando aprovação\tОжидают утверждения\t승인 대기\tMenunggu persetujuan\tOnay bekliyor
已审批\tApproved\tAprobadas\tتمت الموافقة\tGenehmigt\tApprouvées\t承認済み\tAprovadas\tУтверждено\t승인됨\tDisetujui\tOnaylandı
所有申请\tAll requests\tTodas las solicitudes\tجميع الطلبات\tAlle Anträge\tToutes les demandes\tすべての申請\tTodas as solicitações\tВсе заявки\t모든 요청\tSemua permintaan\tTüm başvurular
相较昨日\tCompared with yesterday\tComparado con ayer\tمقارنة بالأمس\tIm Vergleich zu gestern\tPar rapport à hier\t昨日比\tComparado a ontem\tПо сравнению со вчерашним днём\t어제 대비\tDibandingkan kemarin\tDüne kıyasla
查看引导手册\tView guide\tVer guía\tعرض الدليل\tLeitfaden anzeigen\tVoir le guide\tガイドを見る\tVer guia\tОткрыть руководство\t가이드 보기\tLihat panduan\tKılavuzu görüntüle
常用功能\tCommon features\tFunciones frecuentes\tالميزات الشائعة\tHäufig verwendete Funktionen\tFonctions courantes\tよく使う機能\tRecursos frequentes\tЧасто используемые функции\t자주 사용하는 기능\tFitur umum\tSık kullanılan özellikler
VLS 设备列表\tVLS device list\tLista de dispositivos VLS\tقائمة أجهزة VLS\tVLS-Geräteliste\tListe des appareils VLS\tVLS デバイス一覧\tLista de dispositivos VLS\tСписок устройств VLS\tVLS 장치 목록\tDaftar perangkat VLS\tVLS cihaz listesi
暂无 VLS 设备\tNo VLS devices\tNo hay dispositivos VLS\tلا توجد أجهزة VLS\tKeine VLS-Geräte\tAucun appareil VLS\tVLS デバイスはありません\tNenhum dispositivo VLS\tНет устройств VLS\tVLS 장치 없음\tTidak ada perangkat VLS\tVLS cihazı yok
播放\tPlay\tReproducir\tتشغيل\tWiedergeben\tLire\t再生\tReproduzir\tВоспроизвести\t재생\tPutar\tOynat
我的待审核\tMy pending reviews\tMis revisiones pendientes\tمراجعاتي المعلقة\tMeine ausstehenden Prüfungen\tMes validations en attente\t自分の確認待ち\tMinhas revisões pendentes\tМои ожидающие проверки\t내 검토 대기\tTinjauan tertunda saya\tBekleyen incelemelerim
设备名称\tDevice name\tNombre del dispositivo\tاسم الجهاز\tGerätename\tNom de l’appareil\tデバイス名\tNome do dispositivo\tИмя устройства\t장치 이름\tNama perangkat\tCihaz adı
标签\tTag\tEtiqueta\tالوسم\tTag\tÉtiquette\tタグ\tEtiqueta\tМетка\t태그\tTag\tEtiket
设备ID\tDevice ID\tID del dispositivo\tمعرّف الجهاز\tGeräte-ID\tID de l’appareil\tデバイス ID\tID do dispositivo\tID устройства\t장치 ID\tID perangkat\tCihaz kimliği
设备类型\tDevice type\tTipo de dispositivo\tنوع الجهاز\tGerätetyp\tType d’appareil\tデバイスタイプ\tTipo de dispositivo\tТип устройства\t장치 유형\tJenis perangkat\tCihaz türü
设备位置\tDevice location\tUbicación del dispositivo\tموقع الجهاز\tGerätestandort\tEmplacement de l’appareil\tデバイスの場所\tLocalização do dispositivo\tРасположение устройства\t장치 위치\tLokasi perangkat\tCihaz konumu
拥有算法\tAssigned algorithms\tAlgoritmos asignados\tالخوارزميات المعينة\tZugewiesene Algorithmen\tAlgorithmes attribués\t割り当て済みアルゴリズム\tAlgoritmos atribuídos\tНазначенные алгоритмы\t할당된 알고리즘\tAlgoritme yang ditetapkan\tAtanan algoritmalar
申请人\tApplicant\tSolicitante\tمقدم الطلب\tAntragsteller\tDemandeur\t申請者\tSolicitante\tЗаявитель\t신청자\tPemohon\tBaşvuru sahibi
详情\tDetails\tDetalles\tالتفاصيل\tDetails\tDétails\t詳細\tDetalhes\tСведения\t세부 정보\tDetail\tAyrıntılar
摄像头预览\tCamera preview\tVista previa de la cámara\tمعاينة الكاميرا\tKameravorschau\tAperçu de la caméra\tカメラプレビュー\tPrévia da câmera\tПредпросмотр камеры\t카메라 미리보기\tPratinjau kamera\tKamera önizleme
用户\tUser\tUsuario\tالمستخدم\tBenutzer\tUtilisateur\tユーザー\tUsuário\tПользователь\t사용자\tPengguna\tKullanıcı
早上好\tGood morning\tBuenos días\tصباح الخير\tGuten Morgen\tBonjour\tおはようございます\tBom dia\tДоброе утро\t좋은 아침입니다\tSelamat pagi\tGünaydın
中午好\tGood afternoon\tBuenas tardes\tمساء الخير\tGuten Tag\tBonjour\tこんにちは\tBoa tarde\tДобрый день\t안녕하세요\tSelamat siang\tİyi günler
下午好\tGood afternoon\tBuenas tardes\tمساء الخير\tGuten Tag\tBonjour\tこんにちは\tBoa tarde\tДобрый день\t좋은 오후입니다\tSelamat sore\tİyi günler
晚上好\tGood evening\tBuenas noches\tمساء الخير\tGuten Abend\tBonsoir\tこんばんは\tBoa noite\tДобрый вечер\t좋은 저녁입니다\tSelamat malam\tİyi akşamlar
夜深了\tGood night\tBuenas noches\tليلة سعيدة\tGute Nacht\tBonne nuit\tおやすみなさい\tBoa noite\tДоброй ночи\t안녕히 주무세요\tSelamat malam\tİyi geceler
算法超市\tAlgorithm marketplace\tMercado de algoritmos\tسوق الخوارزميات\tAlgorithmen-Marktplatz\tMarché des algorithmes\tアルゴリズムマーケット\tMercado de algoritmos\tМагазин алгоритмов\t알고리즘 마켓\tPasar algoritme\tAlgoritma pazarı
算法隧道\tAlgorithm tunnel\tTúnel de algoritmos\tنفق الخوارزميات\tAlgorithmen-Tunnel\tTunnel d’algorithmes\tアルゴリズムトンネル\tTúnel de algoritmos\tТуннель алгоритмов\t알고리즘 터널\tTerowongan algoritme\tAlgoritma tüneli
操作日志\tOperation log\tRegistro de operaciones\tسجل العمليات\tVorgangsprotokoll\tJournal des opérations\t操作ログ\tLog de operações\tЖурнал операций\t작업 로그\tLog operasi\tİşlem günlüğü
算法自主训练\tSelf-service algorithm training\tEntrenamiento autónomo de algoritmos\tتدريب الخوارزميات الذاتي\tEigenständiges Algorithmustraining\tEntraînement autonome des algorithmes\tアルゴリズムのセルフトレーニング\tTreinamento autônomo de algoritmos\tСамостоятельное обучение алгоритмов\t알고리즘 자율 학습\tPelatihan algoritme mandiri\tBağımsız algoritma eğitimi
算法编排\tAlgorithm orchestration\tOrquestación de algoritmos\tتنسيق الخوارزميات\tAlgorithmen-Orchestrierung\tOrchestration des algorithmes\tアルゴリズムオーケストレーション\tOrquestração de algoritmos\tОркестрация алгоритмов\t알고리즘 오케스트레이션\tOrkestrasi algoritme\tAlgoritma orkestrasyonu
切换全屏失败\tCould not switch fullscreen mode\tNo se pudo cambiar el modo de pantalla completa\tتعذر تغيير وضع ملء الشاشة\tVollbildmodus konnte nicht gewechselt werden\tImpossible de changer le mode plein écran\t全画面モードを切り替えられませんでした\tNão foi possível alternar o modo de tela cheia\tНе удалось переключить полноэкранный режим\t전체 화면 모드를 전환하지 못했습니다\tTidak dapat mengganti mode layar penuh\tTam ekran modu değiştirilemedi
详情功能开发中\tDetails are under development\tLa función de detalles está en desarrollo\tميزة التفاصيل قيد التطوير\tDetailfunktion wird entwickelt\tLa fonction de détails est en cours de développement\t詳細機能は開発中です\tA função de detalhes está em desenvolvimento\tРаздел сведений находится в разработке\t세부 정보 기능을 개발 중입니다\tFitur detail sedang dikembangkan\tAyrıntılar özelliği geliştiriliyor
编辑功能开发中\tEditing is under development\tLa función de edición está en desarrollo\tميزة التحرير قيد التطوير\tBearbeitungsfunktion wird entwickelt\tLa fonction de modification est en cours de développement\t編集機能は開発中です\tA função de edição está em desenvolvimento\tРедактирование находится в разработке\t편집 기능을 개발 중입니다\tFitur edit sedang dikembangkan\tDüzenleme özelliği geliştiriliyor
删除功能开发中\tDeletion is under development\tLa función de eliminación está en desarrollo\tميزة الحذف قيد التطوير\tLöschfunktion wird entwickelt\tLa fonction de suppression est en cours de développement\t削除機能は開発中です\tA função de exclusão está em desenvolvimento\tУдаление находится в разработке\t삭제 기능을 개발 중입니다\tFitur hapus sedang dikembangkan\tSilme özelliği geliştiriliyor
加载 VLS 设备列表失败\tCould not load the VLS device list\tNo se pudo cargar la lista de dispositivos VLS\tتعذر تحميل قائمة أجهزة VLS\tVLS-Geräteliste konnte nicht geladen werden\tImpossible de charger la liste des appareils VLS\tVLS デバイス一覧を読み込めませんでした\tNão foi possível carregar a lista de dispositivos VLS\tНе удалось загрузить список устройств VLS\tVLS 장치 목록을 불러오지 못했습니다\tTidak dapat memuat daftar perangkat VLS\tVLS cihaz listesi yüklenemedi
管理员\tAdministrator\tAdministrador\tالمسؤول\tAdministrator\tAdministrateur\t管理者\tAdministrador\tАдминистратор\t관리자\tAdministrator\tYönetici
加载中...\tLoading...\tCargando...\tجارٍ التحميل...\tWird geladen...\tChargement...\t読み込み中...\tCarregando...\tЗагрузка...\t불러오는 중...\tMemuat...\tYükleniyor...
未登录\tNot signed in\tSesión no iniciada\tلم يتم تسجيل الدخول\tNicht angemeldet\tNon connecté\t未ログイン\tNão conectado\tВход не выполнен\t로그인하지 않음\tBelum masuk\tOturum açılmadı
加载失败\tLoading failed\tError al cargar\tفشل التحميل\tLaden fehlgeschlagen\tÉchec du chargement\t読み込みに失敗しました\tFalha ao carregar\tОшибка загрузки\t불러오기 실패\tGagal memuat\tYükleme başarısız
默认租户\tDefault tenant\tInquilino predeterminado\tالمستأجر الافتراضي\tStandardmandant\tLocataire par défaut\tデフォルトテナント\tLocatário padrão\tАрендатор по умолчанию\t기본 테넌트\tTenant default\tVarsayılan kiracı
未知租户\tUnknown tenant\tInquilino desconocido\tمستأجر غير معروف\tUnbekannter Mandant\tLocataire inconnu\t不明なテナント\tLocatário desconhecido\tНеизвестный арендатор\t알 수 없는 테넌트\tTenant tidak dikenal\tBilinmeyen kiracı
暂无租户\tNo tenants\tNo hay inquilinos\tلا يوجد مستأجرون\tKeine Mandanten\tAucun locataire\tテナントなし\tNenhum locatário\tНет арендаторов\t테넌트 없음\tTidak ada tenant\tKiracı yok
获取租户信息失败\tCould not load tenant information\tNo se pudo cargar la información del inquilino\tتعذر تحميل معلومات المستأجر\tMandanteninformationen konnten nicht geladen werden\tImpossible de charger les informations du locataire\tテナント情報を取得できませんでした\tNão foi possível carregar as informações do locatário\tНе удалось загрузить сведения об арендаторе\t테넌트 정보를 불러오지 못했습니다\tTidak dapat memuat informasi tenant\tKiracı bilgileri yüklenemedi
端侧智能\tEdge AI\tIA en el borde\tالذكاء الاصطناعي الطرفي\tEdge-KI\tIA en périphérie\tエッジ AI\tIA de borda\tПериферийный ИИ\t엣지 AI\tAI edge\tUç yapay zekâ
工单管理\tWork order management\tAdministración de órdenes de trabajo\tإدارة أوامر العمل\tArbeitsauftragsverwaltung\tGestion des ordres de travail\t作業指示管理\tGerenciamento de ordens de serviço\tУправление нарядами\t작업 지시 관리\tManajemen perintah kerja\tİş emri yönetimi
国标协议\tGB28181 protocol\tProtocolo GB28181\tبروتوكول GB28181\tGB28181-Protokoll\tProtocole GB28181\tGB28181 プロトコル\tProtocolo GB28181\tПротокол GB28181\tGB28181 프로토콜\tProtokol GB28181\tGB28181 protokolü
已退出登录\tSigned out\tSesión cerrada\tتم تسجيل الخروج\tAbgemeldet\tDéconnecté\tログアウトしました\tSessão encerrada\tВы вышли из системы\t로그아웃했습니다\tBerhasil keluar\tÇıkış yapıldı
云端智能\tCloud AI\tIA en la nube\tالذكاء الاصطناعي السحابي\tCloud-KI\tIA cloud\tクラウド AI\tIA na nuvem\tОблачный ИИ\t클라우드 AI\tAI cloud\tBulut yapay zekâ
智能分析\tIntelligent analysis\tAnálisis inteligente\tالتحليل الذكي\tIntelligente Analyse\tAnalyse intelligente\tインテリジェント分析\tAnálise inteligente\tИнтеллектуальный анализ\t지능형 분석\tAnalisis cerdas\tAkıllı analiz
主动安全\tActive safety\tSeguridad activa\tالسلامة النشطة\tAktive Sicherheit\tSécurité active\tアクティブセーフティ\tSegurança ativa\tАктивная безопасность\t능동 안전\tKeselamatan aktif\tAktif güvenlik
自定义\tCustom\tPersonalizado\tمخصص\tBenutzerdefiniert\tPersonnalisé\tカスタム\tPersonalizado\tПользовательский\t사용자 지정\tKhusus\tÖzel
租户切换成功\tTenant switched successfully\tInquilino cambiado correctamente\tتم تبديل المستأجر بنجاح\tMandant erfolgreich gewechselt\tLocataire changé avec succès\tテナントを切り替えました\tLocatário alterado com sucesso\tАрендатор успешно сменён\t테넌트가 전환되었습니다\tTenant berhasil diganti\tKiracı başarıyla değiştirildi
VLStream协议\tVLStream protocol\tProtocolo VLStream\tبروتوكول VLStream\tVLStream-Protokoll\tProtocole VLStream\tVLStream プロトコル\tProtocolo VLStream\tПротокол VLStream\tVLStream 프로토콜\tProtokol VLStream\tVLStream protokolü
ISUP协议\tISUP protocol\tProtocolo ISUP\tبروتوكول ISUP\tISUP-Protokoll\tProtocole ISUP\tISUP プロトコル\tProtocolo ISUP\tПротокол ISUP\tISUP 프로토콜\tProtokol ISUP\tISUP protokolü
EHome协议\tEHome protocol\tProtocolo EHome\tبروتوكول EHome\tEHome-Protokoll\tProtocole EHome\tEHome プロトコル\tProtocolo EHome\tПротокол EHome\tEHome 프로토콜\tProtokol EHome\tEHome protokolü
RTSP协议\tRTSP protocol\tProtocolo RTSP\tبروتوكول RTSP\tRTSP-Protokoll\tProtocole RTSP\tRTSP プロトコル\tProtocolo RTSP\tПротокол RTSP\tRTSP 프로토콜\tProtokol RTSP\tRTSP protokolü
ONVIF协议\tONVIF protocol\tProtocolo ONVIF\tبروتوكول ONVIF\tONVIF-Protokoll\tProtocole ONVIF\tONVIF プロトコル\tProtocolo ONVIF\tПротокол ONVIF\tONVIF 프로토콜\tProtokol ONVIF\tONVIF protokolü
VLS协议设备固件管理\tVLS device firmware management\tAdministración de firmware de dispositivos VLS\tإدارة البرامج الثابتة لأجهزة VLS\tVLS-Gerätefirmwareverwaltung\tGestion des micrologiciels des appareils VLS\tVLS デバイスファームウェア管理\tGerenciamento de firmware de dispositivos VLS\tУправление прошивками устройств VLS\tVLS 장치 펌웨어 관리\tManajemen firmware perangkat VLS\tVLS cihaz ürün yazılımı yönetimi
操作成功\tOperation successful\tOperación correcta\tتمت العملية بنجاح\tVorgang erfolgreich\tOpération réussie\t操作が完了しました\tOperação concluída\tОперация выполнена\t작업 성공\tOperasi berhasil\tİşlem başarılı
场景\tScene\tEscenario\tالسيناريو\tSzenario\tScénario\tシーン\tCenário\tСценарий\t시나리오\tSkenario\tSenaryo
创建人\tCreator\tCreador\tالمنشئ\tErsteller\tCréateur\t作成者\tCriador\tСоздатель\t생성자\tPembuat\tOluşturan
当前选择\tCurrent selection\tSelección actual\tالتحديد الحالي\tAktuelle Auswahl\tSélection actuelle\t現在の選択\tSeleção atual\tТекущий выбор\t현재 선택\tPilihan saat ini\tGeçerli seçim
当前账号\tCurrent account\tCuenta actual\tالحساب الحالي\tAktuelles Konto\tCompte actuel\t現在のアカウント\tConta atual\tТекущий аккаунт\t현재 계정\tAkun saat ini\tGeçerli hesap
当前租户未配置相关配置\tNo related configuration is available for this tenant\tEste inquilino no tiene una configuración relacionada\tلا يتوفر تكوين ذو صلة لهذا المستأجر\tFür diesen Mandanten ist keine entsprechende Konfiguration verfügbar\tAucune configuration associée n’est disponible pour ce locataire\tこのテナントには関連設定がありません\tNão há configuração relacionada para este locatário\tДля этого арендатора нет соответствующей конфигурации\t이 테넌트에 관련 구성이 없습니다\tTidak ada konfigurasi terkait untuk tenant ini\tBu kiracı için ilgili yapılandırma yok
登录更多账号\tSign in to another account\tIniciar sesión con otra cuenta\tتسجيل الدخول بحساب آخر\tMit einem weiteren Konto anmelden\tSe connecter avec un autre compte\t別のアカウントでログイン\tEntrar com outra conta\tВойти в другой аккаунт\t다른 계정으로 로그인\tMasuk dengan akun lain\tBaşka bir hesapla giriş yap
反馈\tFeedback\tComentarios\tالملاحظات\tFeedback\tCommentaires\tフィードバック\tFeedback\tОбратная связь\t피드백\tMasukan\tGeri bildirim
切换成功\tSwitched successfully\tCambio correcto\tتم التبديل بنجاح\tErfolgreich gewechselt\tChangement réussi\t切り替えました\tAlteração concluída\tПереключено\t전환되었습니다\tBerhasil diganti\tBaşarıyla değiştirildi
切换行业/场景/职能\tSwitch industry, scene, or function\tCambiar industria, escenario o función\tتغيير القطاع أو السيناريو أو الوظيفة\tBranche, Szenario oder Funktion wechseln\tChanger de secteur, de scénario ou de fonction\t業界・シーン・職能を切り替え\tAlterar setor, cenário ou função\tСменить отрасль, сценарий или функцию\t산업, 시나리오 또는 기능 전환\tGanti industri, skenario, atau fungsi\tSektör, senaryo veya işlev değiştir
切换主题色\tChange theme color\tCambiar color del tema\tتغيير لون السمة\tDesignfarbe ändern\tChanger la couleur du thème\tテーマカラーを変更\tAlterar cor do tema\tСменить цвет темы\t테마 색상 변경\tGanti warna tema\tTema rengini değiştir
清除未读\tMark all as read\tMarcar todo como leído\tتعليم الكل كمقروء\tAlle als gelesen markieren\tTout marquer comme lu\tすべて既読にする\tMarcar tudo como lido\tОтметить всё как прочитанное\t모두 읽음으로 표시\tTandai semua sudah dibaca\tTümünü okundu işaretle
确定退出登录吗？\tAre you sure you want to sign out?\t¿Seguro que quieres cerrar sesión?\tهل تريد بالتأكيد تسجيل الخروج؟\tMöchten Sie sich wirklich abmelden?\tVoulez-vous vraiment vous déconnecter ?\tログアウトしますか？\tTem certeza de que deseja sair?\tВыйти из системы?\t로그아웃하시겠습니까?\tYakin ingin keluar?\tÇıkış yapmak istediğinizden emin misiniz?
我的账号\tMy account\tMi cuenta\tحسابي\tMein Konto\tMon compte\t自分のアカウント\tMinha conta\tМой аккаунт\t내 계정\tAkun saya\tHesabım
我的组织\tMy organization\tMi organización\tمؤسستي\tMeine Organisation\tMon organisation\t自分の組織\tMinha organização\tМоя организация\t내 조직\tOrganisasi saya\tKuruluşum
消息详情\tMessage details\tDetalles del mensaje\tتفاصيل الرسالة\tNachrichtendetails\tDétails du message\tメッセージの詳細\tDetalhes da mensagem\tСведения о сообщении\t메시지 세부 정보\tDetail pesan\tMesaj ayrıntıları
行业\tIndustry\tIndustria\tالقطاع\tBranche\tSecteur\t業界\tSetor\tОтрасль\t산업\tIndustri\tSektör
隐私\tPrivacy\tPrivacidad\tالخصوصية\tDatenschutz\tConfidentialité\tプライバシー\tPrivacidade\tКонфиденциальность\t개인정보 보호\tPrivasi\tGizlilik
用户信息已失效，请重新登录\tYour session is no longer valid. Sign in again\tTu sesión ya no es válida. Inicia sesión de nuevo\tلم تعد جلستك صالحة. سجل الدخول مرة أخرى\tIhre Sitzung ist nicht mehr gültig. Melden Sie sich erneut an\tVotre session n’est plus valide. Reconnectez-vous\tセッションが無効です。もう一度ログインしてください\tSua sessão não é mais válida. Entre novamente\tСеанс недействителен. Войдите снова\t세션이 유효하지 않습니다. 다시 로그인하세요\tSesi tidak lagi valid. Silakan masuk kembali\tOturumunuz artık geçerli değil. Tekrar giriş yapın
暂无其他账号\tNo other accounts\tNo hay otras cuentas\tلا توجد حسابات أخرى\tKeine weiteren Konten\tAucun autre compte\t他のアカウントはありません\tNenhuma outra conta\tДругих аккаунтов нет\t다른 계정 없음\tTidak ada akun lain\tBaşka hesap yok
暂无其他组织\tNo other organizations\tNo hay otras organizaciones\tلا توجد مؤسسات أخرى\tKeine weiteren Organisationen\tAucune autre organisation\t他の組織はありません\tNenhuma outra organização\tДругих организаций нет\t다른 조직 없음\tTidak ada organisasi lain\tBaşka kuruluş yok
暂无应用\tNo apps\tNo hay aplicaciones\tلا توجد تطبيقات\tKeine Apps\tAucune application\tアプリはありません\tNenhum aplicativo\tНет приложений\t앱 없음\tTidak ada aplikasi\tUygulama yok
职能\tFunction\tFunción\tالوظيفة\tFunktion\tFonction\t職能\tFunção\tФункция\t기능\tFungsi\tİşlev
租户\tTenant\tInquilino\tالمستأجر\tMandant\tLocataire\tテナント\tLocatário\tАрендатор\t테넌트\tTenant\tKiracı
区域\tRegion\tRegión\tالمنطقة\tRegion\tRégion\t地域\tRegião\tРегион\t지역\tWilayah\tBölge
分组\tGroup\tGrupo\tالمجموعة\tGruppe\tGroupe\tグループ\tGrupo\tГруппа\t그룹\tGrup\tGrup
全部\tAll\tTodo\tالكل\tAlle\tTout\tすべて\tTudo\tВсе\t전체\tSemua\tTümü
正在处理\tIn progress\tEn proceso\tقيد المعالجة\tIn Bearbeitung\tEn cours\t処理中\tEm andamento\tВ обработке\t처리 중\tSedang diproses\tİşleniyor
已完成\tCompleted\tCompletado\tمكتمل\tAbgeschlossen\tTerminé\t完了\tConcluído\tЗавершено\t완료\tSelesai\tTamamlandı
导出\tExport\tExportar\tتصدير\tExportieren\tExporter\tエクスポート\tExportar\tЭкспорт\t내보내기\tEkspor\tDışa aktar
序号\tNo.\tN.º\tالرقم\tNr.\tN°\t番号\tNº\t№\t번호\tNo.\tNo.
事件时间\tEvent time\tHora del evento\tوقت الحدث\tEreigniszeit\tHeure de l’événement\tイベント時刻\tHora do evento\tВремя события\t이벤트 시간\tWaktu peristiwa\tOlay zamanı
事件位置\tEvent location\tUbicación del evento\tموقع الحدث\tEreignisort\tLieu de l’événement\tイベント場所\tLocal do evento\tМесто события\t이벤트 위치\tLokasi peristiwa\tOlay konumu
事件类型\tEvent type\tTipo de evento\tنوع الحدث\tEreignistyp\tType d’événement\tイベントタイプ\tTipo de evento\tТип события\t이벤트 유형\tJenis peristiwa\tOlay türü
抓拍照片\tSnapshot\tCaptura\tصورة ملتقطة\tSchnappschuss\tCapture\tスナップショット\tCaptura\tСнимок\t스냅샷\tTangkapan\tAnlık görüntü
录制视频\tRecorded video\tVideo grabado\tفيديو مسجل\tAufgezeichnetes Video\tVidéo enregistrée\t録画映像\tVídeo gravado\tЗаписанное видео\t녹화 영상\tVideo rekaman\tKaydedilmiş video
设备编号\tDevice number\tNúmero del dispositivo\tرقم الجهاز\tGerätenummer\tNuméro de l’appareil\tデバイス番号\tNúmero do dispositivo\tНомер устройства\t장치 번호\tNomor perangkat\tCihaz numarası
事件描述\tEvent description\tDescripción del evento\tوصف الحدث\tEreignisbeschreibung\tDescription de l’événement\tイベントの説明\tDescrição do evento\tОписание события\t이벤트 설명\tDeskripsi peristiwa\tOlay açıklaması
完成时间\tCompletion time\tHora de finalización\tوقت الاكتمال\tAbschlusszeit\tHeure de fin\t完了時刻\tHora de conclusão\tВремя завершения\t완료 시간\tWaktu selesai\tTamamlanma zamanı
暂无视频\tNo video\tSin video\tلا يوجد فيديو\tKein Video\tAucune vidéo\t動画なし\tSem vídeo\tНет видео\t영상 없음\tTidak ada video\tVideo yok
搜索\tSearch\tBuscar\tبحث\tSuchen\tRechercher\t検索\tPesquisar\tПоиск\t검색\tCari\tAra
`

export const phraseCatalog = Object.fromEntries(localeColumns.map(code => [code, {}]))

catalogTsv.trim().split('\n').forEach((line, lineIndex) => {
  const columns = line.split('\t')
  if (columns.length !== localeColumns.length) {
    throw new Error(`Invalid i18n catalog row ${lineIndex + 1}: expected ${localeColumns.length} columns, received ${columns.length}`)
  }
  const source = columns[0]
  localeColumns.forEach((code, index) => {
    phraseCatalog[code][source] = columns[index]
  })
})

export const messages = Object.fromEntries(localeColumns.map(code => [code, {
  app: {
    name: 'VLStream Cloud',
    language: phraseCatalog[code]['语言']
  }
}]))
