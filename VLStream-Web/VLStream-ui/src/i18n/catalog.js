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
暂无数据，点击新增\tNo data. Click to add\tSin datos. Haz clic para agregar\tلا توجد بيانات. انقر للإضافة\tKeine Daten. Zum Hinzufügen klicken\tAucune donnée. Cliquez pour ajouter\tデータがありません。クリックして追加\tSem dados. Clique para adicionar\tНет данных. Нажмите, чтобы добавить\t데이터가 없습니다. 클릭하여 추가하세요\tTidak ada data. Klik untuk menambah\tVeri yok. Eklemek için tıklayın
设置分类\tSet classification\tConfigurar clasificación\tتعيين التصنيف\tKlassifizierung festlegen\tDéfinir la classification\t分類を設定\tDefinir classificação\tНастроить классификацию\t분류 설정\tAtur klasifikasi\tSınıflandırmayı ayarla
勾选一台可单独设置，勾选多台可批量设置\tSelect one device for individual settings or multiple devices for batch settings\tSelecciona un dispositivo para configurarlo por separado o varios para configurarlos por lote\tحدد جهازًا واحدًا للإعداد الفردي أو عدة أجهزة للإعداد الجماعي\tEin Gerät einzeln oder mehrere Geräte gleichzeitig konfigurieren\tSélectionnez un appareil pour un réglage individuel ou plusieurs pour un réglage groupé\t1台なら個別設定、複数台なら一括設定できます\tSelecione um dispositivo para configuração individual ou vários para configuração em lote\tВыберите одно устройство для отдельной настройки или несколько для групповой\t장치 하나는 개별 설정, 여러 장치는 일괄 설정할 수 있습니다\tPilih satu perangkat untuk pengaturan individual atau beberapa untuk pengaturan massal\tTek cihazı ayrı, birden çok cihazı toplu olarak ayarlayın
新增\tAdd\tAgregar\tإضافة\tHinzufügen\tAjouter\t追加\tAdicionar\tДобавить\t추가\tTambah\tEkle
修改\tEdit\tModificar\tتعديل\tBearbeiten\tModifier\t変更\tAlterar\tИзменить\t수정\tUbah\tDeğiştir
上级节点\tParent node\tNodo superior\tالعقدة الأصل\tÜbergeordneter Knoten\tNœud parent\t親ノード\tNó superior\tРодительский узел\t상위 노드\tNode induk\tÜst düğüm
分类名称\tClassification name\tNombre de la clasificación\tاسم التصنيف\tKlassifizierungsname\tNom de la classification\t分類名\tNome da classificação\tНазвание классификации\t분류 이름\tNama klasifikasi\tSınıflandırma adı
显示顺序\tDisplay order\tOrden de visualización\tترتيب العرض\tAnzeigereihenfolge\tOrdre d’affichage\t表示順\tOrdem de exibição\tПорядок отображения\t표시 순서\tUrutan tampilan\tGörüntüleme sırası
设置设备分类\tSet device classification\tConfigurar clasificación de dispositivos\tتعيين تصنيف الجهاز\tGeräteklassifizierung festlegen\tDéfinir la classification des appareils\tデバイス分類を設定\tDefinir classificação do dispositivo\tНастроить классификацию устройств\t장치 분류 설정\tAtur klasifikasi perangkat\tCihaz sınıflandırmasını ayarla
批量设置会用本次选择覆盖这些设备原有的区域、分组和标签\tBatch settings replace the existing region, group, and tags of these devices\tLa configuración por lote reemplazará la región, el grupo y las etiquetas actuales de estos dispositivos\tستستبدل الإعدادات الجماعية المنطقة والمجموعة والوسوم الحالية لهذه الأجهزة\tDie Stapelkonfiguration ersetzt Region, Gruppe und Tags dieser Geräte\tLe réglage groupé remplacera la région, le groupe et les étiquettes actuels de ces appareils\t一括設定すると、これらのデバイスの既存の地域・グループ・タグが置き換わります\tA configuração em lote substituirá a região, o grupo e as etiquetas atuais desses dispositivos\tГрупповая настройка заменит текущий регион, группу и метки этих устройств\t일괄 설정은 해당 장치의 기존 지역, 그룹 및 태그를 대체합니다\tPengaturan massal akan mengganti wilayah, grup, dan tag perangkat ini\tToplu ayar bu cihazların mevcut bölge, grup ve etiketlerini değiştirir
请输入分类名称\tEnter a classification name\tIngresa un nombre de clasificación\tأدخل اسم التصنيف\tKlassifizierungsnamen eingeben\tSaisissez un nom de classification\t分類名を入力してください\tDigite um nome de classificação\tВведите название классификации\t분류 이름을 입력하세요\tMasukkan nama klasifikasi\tSınıflandırma adı girin
顶级节点\tTop-level node\tNodo de nivel superior\tعقدة المستوى الأعلى\tOberster Knoten\tNœud de premier niveau\t最上位ノード\tNó de nível superior\tУзел верхнего уровня\t최상위 노드\tNode tingkat atas\tÜst düzey düğüm
保存成功\tSaved successfully\tGuardado correctamente\tتم الحفظ بنجاح\tErfolgreich gespeichert\tEnregistré avec succès\t保存しました\tSalvo com sucesso\tСохранено\t저장되었습니다\tBerhasil disimpan\tBaşarıyla kaydedildi
删除成功\tDeleted successfully\tEliminado correctamente\tتم الحذف بنجاح\tErfolgreich gelöscht\tSupprimé avec succès\t削除しました\tExcluído com sucesso\tУдалено\t삭제되었습니다\tBerhasil dihapus\tBaşarıyla silindi
分类设置成功\tClassification set successfully\tClasificación configurada correctamente\tتم تعيين التصنيف بنجاح\tKlassifizierung erfolgreich festgelegt\tClassification définie avec succès\t分類を設定しました\tClassificação definida com sucesso\tКлассификация настроена\t분류가 설정되었습니다\tKlasifikasi berhasil diatur\tSınıflandırma başarıyla ayarlandı
确认删除所选分类吗？\tDelete the selected classification?\t¿Eliminar la clasificación seleccionada?\tهل تريد حذف التصنيف المحدد؟\tAusgewählte Klassifizierung löschen?\tSupprimer la classification sélectionnée ?\t選択した分類を削除しますか？\tExcluir a classificação selecionada?\tУдалить выбранную классификацию?\t선택한 분류를 삭제하시겠습니까?\tHapus klasifikasi yang dipilih?\tSeçili sınıflandırma silinsin mi?
新增事件\tAdd event\tAgregar evento\tإضافة حدث\tEreignis hinzufügen\tAjouter un événement\tイベントを追加\tAdicionar evento\tДобавить событие\t이벤트 추가\tTambah peristiwa\tOlay ekle
暂无图片\tNo images\tSin imágenes\tلا توجد صور\tKeine Bilder\tAucune image\t画像なし\tSem imagens\tНет изображений\t이미지 없음\tTidak ada gambar\tGörsel yok
确认事件\tConfirm event\tConfirmar evento\tتأكيد الحدث\tEreignis bestätigen\tConfirmer l’événement\tイベントを確認\tConfirmar evento\tПодтвердить событие\t이벤트 확인\tKonfirmasi peristiwa\tOlayı onayla
视频播放\tVideo playback\tReproducción de video\tتشغيل الفيديو\tVideowiedergabe\tLecture vidéo\t動画再生\tReprodução de vídeo\tВоспроизведение видео\t비디오 재생\tPemutaran video\tVideo oynatma
未知状态\tUnknown status\tEstado desconocido\tحالة غير معروفة\tUnbekannter Status\tÉtat inconnu\t不明なステータス\tStatus desconhecido\tНеизвестный статус\t알 수 없는 상태\tStatus tidak dikenal\tBilinmeyen durum
待确认\tPending confirmation\tPendiente de confirmación\tبانتظار التأكيد\tBestätigung ausstehend\tEn attente de confirmation\t確認待ち\tAguardando confirmação\tОжидает подтверждения\t확인 대기\tMenunggu konfirmasi\tOnay bekliyor
真实告警\tConfirmed alarm\tAlarma confirmada\tإنذار مؤكد\tBestätigter Alarm\tAlarme confirmée\t確認済みアラーム\tAlarme confirmado\tПодтверждённая тревога\t확인된 알람\tAlarm terkonfirmasi\tDoğrulanmış alarm
维保\tMaintenance\tMantenimiento\tالصيانة\tWartung\tMaintenance\t保守\tManutenção\tОбслуживание\t유지보수\tPemeliharaan\tBakım
误报\tFalse alarm\tFalsa alarma\tإنذار كاذب\tFehlalarm\tFausse alarme\t誤報\tAlarme falso\tЛожная тревога\t오탐\tAlarm palsu\tYanlış alarm
获取分类设备失败\tCould not load classified devices\tNo se pudieron cargar los dispositivos clasificados\tتعذر تحميل الأجهزة المصنفة\tKlassifizierte Geräte konnten nicht geladen werden\tImpossible de charger les appareils classés\t分類済みデバイスを読み込めませんでした\tNão foi possível carregar os dispositivos classificados\tНе удалось загрузить классифицированные устройства\t분류된 장치를 불러오지 못했습니다\tTidak dapat memuat perangkat terklasifikasi\tSınıflandırılmış cihazlar yüklenemedi
获取数据失败\tCould not load data\tNo se pudieron cargar los datos\tتعذر تحميل البيانات\tDaten konnten nicht geladen werden\tImpossible de charger les données\tデータを読み込めませんでした\tNão foi possível carregar os dados\tНе удалось загрузить данные\t데이터를 불러오지 못했습니다\tTidak dapat memuat data\tVeriler yüklenemedi
请选择要删除的事件\tSelect events to delete\tSelecciona los eventos que deseas eliminar\tحدد الأحداث المراد حذفها\tZu löschende Ereignisse auswählen\tSélectionnez les événements à supprimer\t削除するイベントを選択してください\tSelecione os eventos a excluir\tВыберите события для удаления\t삭제할 이벤트를 선택하세요\tPilih peristiwa yang akan dihapus\tSilinecek olayları seçin
删除确认\tConfirm deletion\tConfirmar eliminación\tتأكيد الحذف\tLöschen bestätigen\tConfirmer la suppression\t削除の確認\tConfirmar exclusão\tПодтверждение удаления\t삭제 확인\tKonfirmasi penghapusan\tSilmeyi onayla
确定删除所选事件吗？\tDelete the selected events?\t¿Eliminar los eventos seleccionados?\tهل تريد حذف الأحداث المحددة؟\tAusgewählte Ereignisse löschen?\tSupprimer les événements sélectionnés ?\t選択したイベントを削除しますか？\tExcluir os eventos selecionados?\tУдалить выбранные события?\t선택한 이벤트를 삭제하시겠습니까?\tHapus peristiwa yang dipilih?\tSeçili olaylar silinsin mi?
时间\tTime\tHora\tالوقت\tZeit\tHeure\t時刻\tHora\tВремя\t시간\tWaktu\tZaman
基本属性\tBasic information\tInformación básica\tالمعلومات الأساسية\tGrundinformationen\tInformations générales\t基本情報\tInformações básicas\tОсновная информация\t기본 정보\tInformasi dasar\tTemel bilgiler
收起\tCollapse\tContraer\tطي\tEinklappen\tRéduire\t折りたたむ\tRecolher\tСвернуть\t접기\tCiutkan\tDaralt
展开\tExpand\tExpandir\tتوسيع\tAusklappen\tDévelopper\t展開\tExpandir\tРазвернуть\t펼치기\tPerluas\tGenişlet
事件名称\tEvent name\tNombre del evento\tاسم الحدث\tEreignisname\tNom de l’événement\tイベント名\tNome do evento\tНазвание события\t이벤트 이름\tNama peristiwa\tOlay adı
拍传\tUploaded media\tContenido cargado\tالوسائط المرفوعة\tHochgeladene Medien\tMédias envoyés\tアップロードメディア\tMídia enviada\tЗагруженные материалы\t업로드 미디어\tMedia unggahan\tYüklenen medya
无\tNone\tNinguno\tلا يوجد\tKeine\tAucun\tなし\tNenhum\tНет\t없음\tTidak ada\tYok
上报人员\tReporter\tInformante\tالمبلغ\tMeldende Person\tDéclarant\t報告者\tResponsável pelo relato\tОтправитель\t보고자\tPelapor\tBildiren kişi
上报时间\tReport time\tHora del reporte\tوقت الإبلاغ\tMeldezeit\tHeure du signalement\t報告時刻\tHora do relato\tВремя сообщения\t보고 시간\tWaktu pelaporan\tBildirim zamanı
分配人员\tAssign personnel\tAsignar personal\tتعيين الموظفين\tPersonal zuweisen\tAffecter du personnel\t担当者を割り当て\tAtribuir pessoal\tНазначить исполнителей\t담당자 배정\tTetapkan personel\tPersonel ata
执行人员\tAssignee\tResponsable\tالمنفذ\tAusführende Person\tExécutant\t担当者\tResponsável\tИсполнитель\t담당자\tPelaksana\tAtanan kişi
执行人单位\tAssignee department\tUnidad del responsable\tجهة المنفذ\tAbteilung der ausführenden Person\tService de l’exécutant\t担当者の所属\tUnidade do responsável\tПодразделение исполнителя\t담당자 부서\tUnit pelaksana\tAtanan kişinin birimi
描述\tDescription\tDescripción\tالوصف\tBeschreibung\tDescription\t説明\tDescrição\tОписание\t설명\tDeskripsi\tAçıklama
请选择执行人员\tSelect an assignee\tSelecciona un responsable\tحدد المنفذ\tAusführende Person auswählen\tSélectionnez un exécutant\t担当者を選択してください\tSelecione um responsável\tВыберите исполнителя\t담당자를 선택하세요\tPilih pelaksana\tAtanan kişiyi seçin
请选择执行人单位\tSelect the assignee department\tSelecciona la unidad del responsable\tحدد جهة المنفذ\tAbteilung der ausführenden Person auswählen\tSélectionnez le service de l’exécutant\t担当者の所属を選択してください\tSelecione a unidade do responsável\tВыберите подразделение исполнителя\t담당자 부서를 선택하세요\tPilih unit pelaksana\tAtanan kişinin birimini seçin
请输入描述\tEnter a description\tIngresa una descripción\tأدخل وصفًا\tBeschreibung eingeben\tSaisissez une description\t説明を入力してください\tDigite uma descrição\tВведите описание\t설명을 입력하세요\tMasukkan deskripsi\tAçıklama girin
选择执行人员\tSelect assignee\tSeleccionar responsable\tتحديد المنفذ\tAusführende Person auswählen\tSélectionner un exécutant\t担当者を選択\tSelecionar responsável\tВыбрать исполнителя\t담당자 선택\tPilih pelaksana\tAtanan kişiyi seç
提交成功\tSubmitted successfully\tEnviado correctamente\tتم الإرسال بنجاح\tErfolgreich übermittelt\tEnvoyé avec succès\t送信しました\tEnviado com sucesso\tОтправлено\t제출되었습니다\tBerhasil dikirim\tBaşarıyla gönderildi
提交失败\tSubmission failed\tError al enviar\tفشل الإرسال\tÜbermittlung fehlgeschlagen\tÉchec de l’envoi\t送信に失敗しました\tFalha ao enviar\tОшибка отправки\t제출 실패\tGagal mengirim\tGönderim başarısız
事件详情\tEvent details\tDetalles del evento\tتفاصيل الحدث\tEreignisdetails\tDétails de l’événement\tイベント詳細\tDetalhes do evento\tСведения о событии\t이벤트 세부 정보\tDetail peristiwa\tOlay ayrıntıları
设备ID\tDevice ID\tID del dispositivo\tمعرّف الجهاز\tGeräte-ID\tID de l’appareil\tデバイス ID\tID do dispositivo\tID устройства\t장치 ID\tID perangkat\tCihaz kimliği
告警时间\tAlarm time\tHora de la alarma\tوقت الإنذار\tAlarmzeit\tHeure de l’alarme\tアラーム時刻\tHora do alarme\tВремя тревоги\t알람 시간\tWaktu alarm\tAlarm zamanı
事件确认\tEvent confirmation\tConfirmación del evento\tتأكيد الحدث\tEreignisbestätigung\tConfirmation de l’événement\tイベント確認\tConfirmação do evento\tПодтверждение события\t이벤트 확인\tKonfirmasi peristiwa\tOlay onayı
告警确认\tAlarm classification\tClasificación de la alarma\tتصنيف الإنذار\tAlarmklassifizierung\tClassification de l’alarme\tアラーム分類\tClassificação do alarme\tКлассификация тревоги\t알람 분류\tKlasifikasi alarm\tAlarm sınıflandırması
是否转工单\tCreate a work order\tCrear una orden de trabajo\tإنشاء أمر عمل\tArbeitsauftrag erstellen\tCréer un ordre de travail\t作業指示を作成\tCriar uma ordem de serviço\tСоздать наряд\t작업 지시 생성\tBuat perintah kerja\tİş emri oluştur
是\tYes\tSí\tنعم\tJa\tOui\tはい\tSim\tДа\t예\tYa\tEvet
否\tNo\tNo\tلا\tNein\tNon\tいいえ\tNão\tНет\t아니요\tTidak\tHayır
图片\tImages\tImágenes\tالصور\tBilder\tImages\t画像\tImagens\tИзображения\t이미지\tGambar\tGörseller
上传图片\tUpload images\tSubir imágenes\tرفع الصور\tBilder hochladen\tImporter des images\t画像をアップロード\tEnviar imagens\tЗагрузить изображения\t이미지 업로드\tUnggah gambar\tGörsel yükle
请先输入任务名称\tEnter the task name first\tIngresa primero el nombre de la tarea\tأدخل اسم المهمة أولًا\tZuerst den Aufgabennamen eingeben\tSaisissez d’abord le nom de la tâche\t先にタスク名を入力してください\tDigite primeiro o nome da tarefa\tСначала введите название задачи\t먼저 작업 이름을 입력하세요\tMasukkan nama tugas terlebih dahulu\tÖnce görev adını girin
是否完成\tCompleted\tCompletado\tمكتمل\tAbgeschlossen\tTerminé\t完了\tConcluído\tЗавершено\t완료 여부\tSelesai\tTamamlandı
未完成\tNot completed\tSin completar\tغير مكتمل\tNicht abgeschlossen\tNon terminé\t未完了\tNão concluído\tНе завершено\t미완료\tBelum selesai\tTamamlanmadı
反馈列表\tFeedback list\tLista de comentarios\tقائمة الملاحظات\tFeedbackliste\tListe des retours\tフィードバック一覧\tLista de feedback\tСписок отзывов\t피드백 목록\tDaftar masukan\tGeri bildirim listesi
反馈记录\tFeedback history\tHistorial de comentarios\tسجل الملاحظات\tFeedbackverlauf\tHistorique des retours\tフィードバック履歴\tHistórico de feedback\tИстория отзывов\t피드백 기록\tRiwayat masukan\tGeri bildirim geçmişi
工单记录\tWork order history\tHistorial de órdenes de trabajo\tسجل أوامر العمل\tArbeitsauftragsverlauf\tHistorique des ordres de travail\t作業指示履歴\tHistórico de ordens de serviço\tИстория нарядов\t작업 지시 기록\tRiwayat perintah kerja\tİş emri geçmişi
请输入反馈描述\tEnter feedback details\tIngresa los detalles de los comentarios\tأدخل تفاصيل الملاحظات\tFeedbackdetails eingeben\tSaisissez les détails du retour\tフィードバック内容を入力してください\tDigite os detalhes do feedback\tВведите описание отзыва\t피드백 내용을 입력하세요\tMasukkan detail masukan\tGeri bildirim ayrıntılarını girin
请选择告警确认\tSelect an alarm classification\tSelecciona una clasificación de alarma\tحدد تصنيف الإنذار\tAlarmklassifizierung auswählen\tSélectionnez une classification d’alarme\tアラーム分類を選択してください\tSelecione uma classificação de alarme\tВыберите классификацию тревоги\t알람 분류를 선택하세요\tPilih klasifikasi alarm\tAlarm sınıflandırması seçin
反馈成功\tFeedback submitted successfully\tComentarios enviados correctamente\tتم إرسال الملاحظات بنجاح\tFeedback erfolgreich gesendet\tRetour envoyé avec succès\tフィードバックを送信しました\tFeedback enviado com sucesso\tОтзыв отправлен\t피드백이 제출되었습니다\tMasukan berhasil dikirim\tGeri bildirim başarıyla gönderildi
事件反馈\tEvent feedback\tComentarios del evento\tملاحظات الحدث\tEreignisfeedback\tRetour sur l’événement\tイベントフィードバック\tFeedback do evento\tОтзыв по событию\t이벤트 피드백\tMasukan peristiwa\tOlay geri bildirimi
反馈图片\tFeedback images\tImágenes de comentarios\tصور الملاحظات\tFeedbackbilder\tImages du retour\tフィードバック画像\tImagens do feedback\tИзображения отзыва\t피드백 이미지\tGambar masukan\tGeri bildirim görselleri
反馈描述\tFeedback details\tDetalles de los comentarios\tتفاصيل الملاحظات\tFeedbackdetails\tDétails du retour\tフィードバック内容\tDetalhes do feedback\tОписание отзыва\t피드백 내용\tDetail masukan\tGeri bildirim ayrıntıları
选择工单\tSelect work order\tSeleccionar orden de trabajo\tتحديد أمر عمل\tArbeitsauftrag auswählen\tSélectionner un ordre de travail\t作業指示を選択\tSelecionar ordem de serviço\tВыбрать наряд\t작업 지시 선택\tPilih perintah kerja\tİş emri seç
请选择工单\tSelect a work order\tSelecciona una orden de trabajo\tحدد أمر عمل\tArbeitsauftrag auswählen\tSélectionnez un ordre de travail\t作業指示を選択してください\tSelecione uma ordem de serviço\tВыберите наряд\t작업 지시를 선택하세요\tPilih perintah kerja\tBir iş emri seçin
任务分配\tTask assignment\tAsignación de tareas\tتعيين المهام\tAufgabenzuweisung\tAffectation des tâches\tタスク割り当て\tAtribuição de tarefas\tНазначение задачи\t작업 배정\tPenugasan tugas\tGörev atama
进行中\tIn progress\tEn proceso\tقيد التنفيذ\tIn Bearbeitung\tEn cours\t進行中\tEm andamento\tВ процессе\t진행 중\tSedang berlangsung\tDevam ediyor
请先输入描述\tEnter a description first\tIngresa primero una descripción\tأدخل وصفًا أولًا\tZuerst eine Beschreibung eingeben\tSaisissez d’abord une description\t先に説明を入力してください\tDigite primeiro uma descrição\tСначала введите описание\t먼저 설명을 입력하세요\tMasukkan deskripsi terlebih dahulu\tÖnce bir açıklama girin
请先输入描述单词\tEnter a description keyword first\tIngresa primero una palabra clave de descripción\tأدخل كلمة وصف أولًا\tZuerst ein Beschreibungsstichwort eingeben\tSaisissez d’abord un mot-clé de description\t先に説明キーワードを入力してください\tDigite primeiro uma palavra-chave de descrição\tСначала введите ключевое слово описания\t먼저 설명 키워드를 입력하세요\tMasukkan kata kunci deskripsi terlebih dahulu\tÖnce bir açıklama anahtar kelimesi girin
车辆\tVehicle\tVehículo\tالمركبة\tFahrzeug\tVéhicule\t車両\tVeículo\tТранспортное средство\t차량\tKendaraan\tAraç
选项1\tOption 1\tOpción 1\tالخيار 1\tOption 1\tOption 1\tオプション1\tOpção 1\tВариант 1\t옵션 1\tOpsi 1\tSeçenek 1
选项2\tOption 2\tOpción 2\tالخيار 2\tOption 2\tOption 2\tオプション2\tOpção 2\tВариант 2\t옵션 2\tOpsi 2\tSeçenek 2
告警状态\tAlarm status\tEstado de la alarma\tحالة الإنذار\tAlarmstatus\tÉtat de l’alarme\tアラーム状態\tStatus do alarme\tСтатус тревоги\t알람 상태\tStatus alarm\tAlarm durumu
请选择车辆\tSelect a vehicle\tSelecciona un vehículo\tحدد مركبة\tFahrzeug auswählen\tSélectionnez un véhicule\t車両を選択してください\tSelecione um veículo\tВыберите транспортное средство\t차량을 선택하세요\tPilih kendaraan\tBir araç seçin
添加为常用语\tAdd to common phrases\tAgregar a frases frecuentes\tإضافة إلى العبارات الشائعة\tZu häufigen Formulierungen hinzufügen\tAjouter aux expressions fréquentes\t定型文に追加\tAdicionar às frases frequentes\tДобавить в частые фразы\t자주 쓰는 문구에 추가\tTambahkan ke frasa umum\tSık kullanılan ifadelere ekle
可用\tAvailable\tDisponible\tمتاح\tVerfügbar\tDisponible\t利用可能\tDisponível\tДоступно\t사용 가능\tTersedia\tKullanılabilir
不可用\tUnavailable\tNo disponible\tغير متاح\tNicht verfügbar\tIndisponible\t利用不可\tIndisponível\tНедоступно\t사용 불가\tTidak tersedia\tKullanılamıyor
设备名称 / ID / 序列号\tDevice name / ID / serial number\tNombre / ID / número de serie\tاسم الجهاز / المعرّف / الرقم التسلسلي\tGerätename / ID / Seriennummer\tNom / ID / numéro de série\tデバイス名 / ID / シリアル番号\tNome / ID / número de série\tИмя / ID / серийный номер\t장치 이름 / ID / 일련번호\tNama / ID / nomor seri perangkat\tCihaz adı / kimliği / seri numarası
全部状态\tAll statuses\tTodos los estados\tجميع الحالات\tAlle Status\tTous les états\tすべてのステータス\tTodos os status\tВсе статусы\t모든 상태\tSemua status\tTüm durumlar
在线状态\tOnline status\tEstado de conexión\tحالة الاتصال\tOnline-Status\tÉtat de connexion\tオンライン状態\tStatus online\tСостояние подключения\t온라인 상태\tStatus online\tÇevrimiçi durum
设备型号\tDevice model\tModelo del dispositivo\tطراز الجهاز\tGerätemodell\tModèle de l’appareil\tデバイスモデル\tModelo do dispositivo\tМодель устройства\t장치 모델\tModel perangkat\tCihaz modeli
序列号\tSerial number\tNúmero de serie\tالرقم التسلسلي\tSeriennummer\tNuméro de série\tシリアル番号\tNúmero de série\tСерийный номер\t일련번호\tNomor seri\tSeri numarası
RootFS 版本\tRootFS version\tVersión de RootFS\tإصدار RootFS\tRootFS-Version\tVersion RootFS\tRootFS バージョン\tVersão do RootFS\tВерсия RootFS\tRootFS 버전\tVersi RootFS\tRootFS sürümü
设备能力\tDevice capabilities\tCapacidades del dispositivo\tإمكانات الجهاز\tGerätefunktionen\tCapacités de l’appareil\tデバイス機能\tRecursos do dispositivo\tВозможности устройства\t장치 기능\tKemampuan perangkat\tCihaz yetenekleri
最近上线\tLast online\tÚltima conexión\tآخر اتصال\tZuletzt online\tDernière connexion\t最終オンライン\tÚltima conexão\tПоследнее подключение\t최근 온라인\tTerakhir online\tSon çevrimiçi
最后心跳\tLast heartbeat\tÚltimo latido\tآخر نبضة\tLetzter Heartbeat\tDernier signal\t最終ハートビート\tÚltimo heartbeat\tПоследний heartbeat\t마지막 하트비트\tHeartbeat terakhir\tSon heartbeat
实时预览\tLive preview\tVista previa en vivo\tمعاينة مباشرة\tLive-Vorschau\tAperçu en direct\tライブプレビュー\tPrévia ao vivo\tПредпросмотр в реальном времени\t실시간 미리보기\tPratinjau langsung\tCanlı önizleme
视频流\tVideo stream\tTransmisión de video\tبث الفيديو\tVideostream\tFlux vidéo\tビデオストリーム\tStream de vídeo\tВидеопоток\t비디오 스트림\tStream video\tVideo akışı
请选择视频流\tSelect a video stream\tSelecciona una transmisión de video\tحدد بث فيديو\tVideostream auswählen\tSélectionnez un flux vidéo\tビデオストリームを選択してください\tSelecione um stream de vídeo\tВыберите видеопоток\t비디오 스트림을 선택하세요\tPilih stream video\tBir video akışı seçin
请选择可用视频流\tSelect an available video stream\tSelecciona una transmisión disponible\tحدد بث فيديو متاحًا\tVerfügbaren Videostream auswählen\tSélectionnez un flux vidéo disponible\t利用可能なビデオストリームを選択してください\tSelecione um stream disponível\tВыберите доступный видеопоток\t사용 가능한 비디오 스트림을 선택하세요\tPilih stream video yang tersedia\tKullanılabilir bir video akışı seçin
设备详情\tDevice details\tDetalles del dispositivo\tتفاصيل الجهاز\tGerätedetails\tDétails de l’appareil\tデバイス詳細\tDetalhes do dispositivo\tСведения об устройстве\t장치 세부 정보\tDetail perangkat\tCihaz ayrıntıları
IP 地址\tIP address\tDirección IP\tعنوان IP\tIP-Adresse\tAdresse IP\tIP アドレス\tEndereço IP\tIP-адрес\tIP 주소\tAlamat IP\tIP adresi
固件升级\tFirmware upgrade\tActualización de firmware\tترقية البرنامج الثابت\tFirmware-Upgrade\tMise à niveau du micrologiciel\tファームウェア更新\tAtualização de firmware\tОбновление прошивки\t펌웨어 업그레이드\tPeningkatan firmware\tÜrün yazılımı yükseltme
开机时间\tBoot time\tHora de inicio\tوقت التشغيل\tStartzeit\tHeure de démarrage\t起動時刻\tHora de inicialização\tВремя запуска\t부팅 시간\tWaktu boot\tBaşlatma zamanı
在线时长\tOnline duration\tTiempo en línea\tمدة الاتصال\tOnline-Dauer\tDurée de connexion\tオンライン時間\tTempo online\tВремя в сети\t온라인 시간\tDurasi online\tÇevrimiçi süre
位置坐标\tLocation coordinates\tCoordenadas\tإحداثيات الموقع\tStandortkoordinaten\tCoordonnées\t位置座標\tCoordenadas\tКоординаты\t위치 좌표\tKoordinat lokasi\tKonum koordinatları
设备运行模型\tModels running on device\tModelos en el dispositivo\tالنماذج المشغلة على الجهاز\tModelle auf dem Gerät\tModèles exécutés sur l’appareil\tデバイス実行モデル\tModelos em execução no dispositivo\tМодели на устройстве\t장치 실행 모델\tModel yang berjalan di perangkat\tCihazda çalışan modeller
模型下发\tDeploy model\tImplementar modelo\tنشر النموذج\tModell bereitstellen\tDéployer le modèle\tモデルを配信\tImplantar modelo\tРазвернуть модель\t모델 배포\tTerapkan model\tModeli dağıt
查看模型\tView models\tVer modelos\tعرض النماذج\tModelle anzeigen\tVoir les modèles\tモデルを表示\tVer modelos\tПросмотреть модели\t모델 보기\tLihat model\tModelleri görüntüle
刷新\tRefresh\tActualizar\tتحديث\tAktualisieren\tActualiser\t更新\tAtualizar\tОбновить\t새로고침\tSegarkan\tYenile
设备当前无模型\tNo models are currently on the device\tEl dispositivo no tiene modelos\tلا توجد نماذج على الجهاز حاليًا\tDerzeit keine Modelle auf dem Gerät\tAucun modèle sur l’appareil\tデバイスにモデルはありません\tNenhum modelo no dispositivo\tНа устройстве нет моделей\t장치에 현재 모델이 없습니다\tTidak ada model di perangkat\tCihazda model yok
设备尚未上报模型信息\tThe device has not reported model information\tEl dispositivo aún no informa modelos\tلم يبلغ الجهاز عن معلومات النموذج\tDas Gerät hat noch keine Modellinformationen gemeldet\tL’appareil n’a pas encore signalé de modèles\tデバイスはモデル情報を報告していません\tO dispositivo ainda não informou modelos\tУстройство ещё не сообщило сведения о моделях\t장치가 모델 정보를 보고하지 않았습니다\tPerangkat belum melaporkan informasi model\tCihaz model bilgisi bildirmedi
设备上报的模型列表为空\tThe reported model list is empty\tLa lista de modelos informada está vacía\tقائمة النماذج المبلغ عنها فارغة\tDie gemeldete Modellliste ist leer\tLa liste des modèles signalés est vide\t報告されたモデル一覧は空です\tA lista de modelos informada está vazia\tПолученный список моделей пуст\t보고된 모델 목록이 비어 있습니다\tDaftar model yang dilaporkan kosong\tBildirilen model listesi boş
模型 ID\tModel ID\tID del modelo\tمعرّف النموذج\tModell-ID\tID du modèle\tモデル ID\tID do modelo\tID модели\t모델 ID\tID model\tModel kimliği
模型名称\tModel name\tNombre del modelo\tاسم النموذج\tModellname\tNom du modèle\tモデル名\tNome do modelo\tНазвание модели\t모델 이름\tNama model\tModel adı
格式\tFormat\tFormato\tالتنسيق\tFormat\tFormat\t形式\tFormato\tФормат\t형식\tFormat\tBiçim
当前显示设备最近一次上报的模型，点击刷新查询设备。\tShowing the latest models reported by the device. Click Refresh to query the device.\tSe muestran los últimos modelos informados. Haz clic en Actualizar para consultar el dispositivo.\tتُعرض أحدث النماذج التي أبلغ عنها الجهاز. انقر على تحديث للاستعلام عن الجهاز.\tDie zuletzt vom Gerät gemeldeten Modelle werden angezeigt. Klicken Sie auf Aktualisieren, um das Gerät abzufragen.\tLes derniers modèles signalés par l’appareil sont affichés. Cliquez sur Actualiser pour interroger l’appareil.\tデバイスが最後に報告したモデルを表示しています。更新をクリックしてデバイスを照会してください。\tExibindo os modelos mais recentes informados. Clique em Atualizar para consultar o dispositivo.\tПоказаны последние модели, сообщённые устройством. Нажмите «Обновить», чтобы опросить устройство.\t장치가 마지막으로 보고한 모델입니다. 새로고침을 눌러 장치를 조회하세요.\tMenampilkan model terakhir yang dilaporkan. Klik Segarkan untuk meminta data perangkat.\tCihazın son bildirdiği modeller gösteriliyor. Cihazı sorgulamak için Yenile'ye tıklayın.
当前显示本次设备查询结果。\tShowing the current device query results.\tSe muestran los resultados de la consulta actual.\tتُعرض نتائج الاستعلام الحالي عن الجهاز.\tDie Ergebnisse der aktuellen Geräteabfrage werden angezeigt.\tLes résultats de la requête actuelle sont affichés.\t今回のデバイス照会結果を表示しています。\tExibindo os resultados da consulta atual.\tПоказаны результаты текущего запроса устройства.\t현재 장치 조회 결과입니다.\tMenampilkan hasil permintaan perangkat saat ini.\tGeçerli cihaz sorgusu sonuçları gösteriliyor.
离线时仅供查看。\tRead-only while offline.\tSolo lectura sin conexión.\tللعرض فقط عند عدم الاتصال.\tOffline nur zur Anzeige.\tConsultation uniquement hors ligne.\tオフライン時は閲覧のみです。\tSomente leitura quando offline.\tВ автономном режиме только просмотр.\t오프라인일 때는 조회만 가능합니다.\tHanya dapat dilihat saat offline.\tÇevrimdışıyken salt okunur.
视频源\tVideo sources\tFuentes de video\tمصادر الفيديو\tVideoquellen\tSources vidéo\tビデオソース\tFontes de vídeo\tИсточники видео\t비디오 소스\tSumber video\tVideo kaynakları
设备没有上报视频源\tThe device has not reported video sources\tEl dispositivo no informó fuentes de video\tلم يبلغ الجهاز عن مصادر فيديو\tDas Gerät hat keine Videoquellen gemeldet\tL’appareil n’a signalé aucune source vidéo\tデバイスはビデオソースを報告していません\tO dispositivo não informou fontes de vídeo\tУстройство не сообщило источники видео\t장치가 비디오 소스를 보고하지 않았습니다\tPerangkat belum melaporkan sumber video\tCihaz video kaynağı bildirmedi
通道\tChannel\tCanal\tالقناة\tKanal\tCanal\tチャンネル\tCanal\tКанал\t채널\tKanal\tKanal
码流类型\tStream type\tTipo de transmisión\tنوع البث\tStreamtyp\tType de flux\tストリームタイプ\tTipo de stream\tТип потока\t스트림 유형\tJenis stream\tAkış türü
协议\tProtocol\tProtocolo\tالبروتوكول\tProtokoll\tProtocole\tプロトコル\tProtocolo\tПротокол\t프로토콜\tProtokol\tProtokol
默认流\tDefault stream\tTransmisión predeterminada\tالبث الافتراضي\tStandardstream\tFlux par défaut\tデフォルトストリーム\tStream padrão\tПоток по умолчанию\t기본 스트림\tStream default\tVarsayılan akış
视频源地址\tVideo source URL\tURL de la fuente de video\tعنوان مصدر الفيديو\tVideoquellen-URL\tURL de la source vidéo\tビデオソース URL\tURL da fonte de vídeo\tURL источника видео\t비디오 소스 URL\tURL sumber video\tVideo kaynağı URL'si
未上报\tNot reported\tNo informado\tلم يتم الإبلاغ\tNicht gemeldet\tNon signalé\t未報告\tNão informado\tНе сообщено\t보고되지 않음\tBelum dilaporkan\tBildirilmedi
可用固件升级\tAvailable firmware upgrades\tActualizaciones de firmware disponibles\tترقيات البرنامج الثابت المتاحة\tVerfügbare Firmware-Upgrades\tMises à niveau disponibles\t利用可能なファームウェア更新\tAtualizações de firmware disponíveis\tДоступные обновления прошивки\t사용 가능한 펌웨어 업그레이드\tPeningkatan firmware tersedia\tKullanılabilir ürün yazılımı yükseltmeleri
没有更高版本的兼容固件\tNo newer compatible firmware\tNo hay firmware compatible más reciente\tلا يوجد برنامج ثابت متوافق أحدث\tKeine neuere kompatible Firmware\tAucun micrologiciel compatible plus récent\t新しい互換ファームウェアはありません\tNão há firmware compatível mais recente\tНет более новой совместимой прошивки\t더 새로운 호환 펌웨어가 없습니다\tTidak ada firmware kompatibel yang lebih baru\tDaha yeni uyumlu ürün yazılımı yok
目标\tTarget\tObjetivo\tالهدف\tZiel\tCible\t対象\tDestino\tЦель\t대상\tTarget\tHedef
当前版本\tCurrent version\tVersión actual\tالإصدار الحالي\tAktuelle Version\tVersion actuelle\t現在のバージョン\tVersão atual\tТекущая версия\t현재 버전\tVersi saat ini\tGeçerli sürüm
最新版本\tLatest version\tVersión más reciente\tأحدث إصدار\tNeueste Version\tDernière version\t最新バージョン\tVersão mais recente\tПоследняя версия\t최신 버전\tVersi terbaru\tEn son sürüm
固件包\tFirmware package\tPaquete de firmware\tحزمة البرنامج الثابت\tFirmwarepaket\tPaquet de micrologiciel\tファームウェアパッケージ\tPacote de firmware\tПакет прошивки\t펌웨어 패키지\tPaket firmware\tÜrün yazılımı paketi
最近 OTA 任务\tLatest OTA task\tTarea OTA más reciente\tأحدث مهمة OTA\tLetzte OTA-Aufgabe\tDernière tâche OTA\t最新の OTA タスク\tTarefa OTA mais recente\tПоследняя задача OTA\t최근 OTA 작업\tTugas OTA terbaru\tSon OTA görevi
终止任务\tEnd task\tFinalizar tarea\tإنهاء المهمة\tAufgabe beenden\tTerminer la tâche\tタスクを終了\tEncerrar tarefa\tЗавершить задачу\t작업 종료\tHentikan tugas\tGörevi sonlandır
版本\tVersion\tVersión\tالإصدار\tVersion\tVersion\tバージョン\tVersão\tВерсия\t버전\tVersi\tSürüm
任务 ID\tTask ID\tID de tarea\tمعرّف المهمة\tAufgaben-ID\tID de tâche\tタスク ID\tID da tarefa\tID задачи\t작업 ID\tID tugas\tGörev kimliği
说明\tDetails\tDetalles\tالتفاصيل\tDetails\tDétails\t説明\tDetalhes\tОписание\t설명\tKeterangan\tAçıklama
设备 ID\tDevice ID\tID del dispositivo\tمعرّف الجهاز\tGeräte-ID\tID de l’appareil\tデバイス ID\tID do dispositivo\tID устройства\t장치 ID\tID perangkat\tCihaz kimliği
设备没有上报可用视频流\tThe device has not reported an available video stream\tEl dispositivo no informó una transmisión disponible\tلم يبلغ الجهاز عن بث فيديو متاح\tDas Gerät hat keinen verfügbaren Videostream gemeldet\tL’appareil n’a signalé aucun flux vidéo disponible\tデバイスは利用可能なビデオストリームを報告していません\tO dispositivo não informou um stream disponível\tУстройство не сообщило доступный видеопоток\t장치가 사용 가능한 비디오 스트림을 보고하지 않았습니다\tPerangkat belum melaporkan stream video yang tersedia\tCihaz kullanılabilir video akışı bildirmedi
设备已确认模型删除成功\tThe device confirmed that the model was deleted\tEl dispositivo confirmó que el modelo se eliminó\tأكد الجهاز حذف النموذج\tDas Gerät hat das Löschen des Modells bestätigt\tL’appareil a confirmé la suppression du modèle\tデバイスがモデルの削除を確認しました\tO dispositivo confirmou a exclusão do modelo\tУстройство подтвердило удаление модели\t장치가 모델 삭제를 확인했습니다\tPerangkat mengonfirmasi penghapusan model\tCihaz modelin silindiğini doğruladı
OTA 固件升级指令已下发\tThe OTA firmware upgrade command was sent\tSe envió el comando de actualización OTA\tتم إرسال أمر ترقية البرنامج الثابت OTA\tDer OTA-Firmware-Upgrade-Befehl wurde gesendet\tLa commande de mise à niveau OTA a été envoyée\tOTA ファームウェア更新コマンドを送信しました\tO comando de atualização OTA foi enviado\tКоманда OTA-обновления отправлена\tOTA 펌웨어 업그레이드 명령이 전송되었습니다\tPerintah peningkatan firmware OTA telah dikirim\tOTA ürün yazılımı yükseltme komutu gönderildi
平台 OTA 任务已终止\tThe platform OTA task was ended\tLa tarea OTA de la plataforma finalizó\tتم إنهاء مهمة OTA على المنصة\tDie Plattform-OTA-Aufgabe wurde beendet\tLa tâche OTA de la plateforme a été terminée\tプラットフォームの OTA タスクを終了しました\tA tarefa OTA da plataforma foi encerrada\tЗадача OTA на платформе завершена\t플랫폼 OTA 작업이 종료되었습니다\tTugas OTA platform telah dihentikan\tPlatform OTA görevi sonlandırıldı
WVP 未返回可用播放地址\tWVP did not return a playable URL\tWVP no devolvió una URL reproducible\tلم يرجع WVP عنوان تشغيل صالحًا\tWVP hat keine abspielbare URL zurückgegeben\tWVP n’a renvoyé aucune URL lisible\tWVP から再生可能な URL が返されませんでした\tO WVP não retornou uma URL reproduzível\tWVP не вернул доступный адрес воспроизведения\tWVP가 재생 가능한 URL을 반환하지 않았습니다\tWVP tidak mengembalikan URL yang dapat diputar\tWVP oynatılabilir bir URL döndürmedi
视频\tVideo\tVideo\tالفيديو\tVideo\tVidéo\t動画\tVídeo\tВидео\t비디오\tVideo\tVideo
音频\tAudio\tAudio\tالصوت\tAudio\tAudio\t音声\tÁudio\tАудио\t오디오\tAudio\tSes
云台\tPTZ\tPTZ\tالتحكم PTZ\tPTZ\tPTZ\tPTZ\tPTZ\tPTZ\tPTZ\tPTZ\tPTZ
AI 推理\tAI inference\tInferencia de IA\tاستدلال الذكاء الاصطناعي\tKI-Inferenz\tInférence IA\tAI 推論\tInferência de IA\tВывод ИИ\tAI 추론\tInferensi AI\tYapay zekâ çıkarımı
人脸识别\tFace recognition\tReconocimiento facial\tالتعرف على الوجوه\tGesichtserkennung\tReconnaissance faciale\t顔認識\tReconhecimento facial\tРаспознавание лиц\t얼굴 인식\tPengenalan wajah\tYüz tanıma
录像\tRecording\tGrabación\tالتسجيل\tAufzeichnung\tEnregistrement\t録画\tGravação\tЗапись\t녹화\tPerekaman\tKayıt
已加载\tLoaded\tCargado\tتم التحميل\tGeladen\tChargé\t読み込み済み\tCarregado\tЗагружено\t로드됨\tDimuat\tYüklendi
运行中\tRunning\tEn ejecución\tقيد التشغيل\tLäuft\tEn cours d’exécution\t実行中\tEm execução\tВыполняется\t실행 중\tBerjalan\tÇalışıyor
已停止\tStopped\tDetenido\tمتوقف\tGestoppt\tArrêté\t停止済み\tParado\tОстановлено\t중지됨\tDihentikan\tDurduruldu
异常\tError\tError\tخطأ\tFehler\tErreur\t異常\tErro\tОшибка\t오류\tKesalahan\tHata
天\tdays\tdías\tأيام\tTage\tjours\t日\tdias\tдн.\t일\thari\tgün
小时\thours\thoras\tساعات\tStd.\theures\t時間\thoras\tч.\t시간\tjam\tsaat
分\tmin\tmin\tدقائق\tMin.\tmin\t分\tmin\tмин.\t분\tmenit\tdk.
秒\tsec\tseg\tثوانٍ\tSek.\ts\t秒\tseg\tсек.\t초\tdetik\tsn.
经度\tLongitude\tLongitud\tخط الطول\tLängengrad\tLongitude\t経度\tLongitude\tДолгота\t경도\tBujur\tBoylam
纬度\tLatitude\tLatitud\tخط العرض\tBreitengrad\tLatitude\t緯度\tLatitude\tШирота\t위도\tLintang\tEnlem
当前显示设备最近一次上报的模型，点击刷新查询设备。离线时仅供查看。\tShowing the latest models reported by the device. Click Refresh to query it. Read-only while offline.\tSe muestran los últimos modelos informados. Haz clic en Actualizar para consultar el dispositivo. Solo lectura sin conexión.\tتُعرض أحدث النماذج المبلغ عنها. انقر على تحديث للاستعلام. للعرض فقط عند عدم الاتصال.\tDie zuletzt gemeldeten Modelle werden angezeigt. Klicken Sie auf Aktualisieren. Offline nur zur Anzeige.\tLes derniers modèles signalés sont affichés. Cliquez sur Actualiser. Consultation uniquement hors ligne.\t最後に報告されたモデルを表示しています。更新して照会してください。オフライン時は閲覧のみです。\tExibindo os modelos mais recentes. Clique em Atualizar. Somente leitura quando offline.\tПоказаны последние модели. Нажмите «Обновить». В автономном режиме только просмотр.\t마지막으로 보고된 모델입니다. 새로고침하여 조회하세요. 오프라인에서는 조회만 가능합니다.\tMenampilkan model terakhir. Klik Segarkan. Hanya dapat dilihat saat offline.\tSon bildirilen modeller gösteriliyor. Yenile'ye tıklayın. Çevrimdışıyken salt okunur.
当前显示本次设备查询结果。离线时仅供查看。\tShowing the current device query results. Read-only while offline.\tSe muestran los resultados actuales. Solo lectura sin conexión.\tتُعرض نتائج الاستعلام الحالي. للعرض فقط عند عدم الاتصال.\tDie aktuellen Abfrageergebnisse werden angezeigt. Offline nur zur Anzeige.\tLes résultats actuels sont affichés. Consultation uniquement hors ligne.\t今回の照会結果を表示しています。オフライン時は閲覧のみです。\tExibindo os resultados atuais. Somente leitura quando offline.\tПоказаны текущие результаты. В автономном режиме только просмотр.\t현재 조회 결과입니다. 오프라인에서는 조회만 가능합니다.\tMenampilkan hasil saat ini. Hanya dapat dilihat saat offline.\tGeçerli sorgu sonuçları gösteriliyor. Çevrimdışıyken salt okunur.
设备管理后台\tDevice administration\tAdministración del dispositivo\tإدارة الجهاز\tGeräteverwaltung\tAdministration de l’appareil\tデバイス管理画面\tAdministração do dispositivo\tАдминистрирование устройства\t장치 관리\tAdministrasi perangkat\tCihaz yönetimi
通过短期安全会话访问，登录仍使用 IPC 自身账号。\tAccess uses a short-lived secure session; sign-in still uses the IPC account.\tEl acceso usa una sesión segura temporal; el inicio de sesión sigue usando la cuenta del IPC.\tيستخدم الوصول جلسة آمنة قصيرة الأجل؛ ويظل تسجيل الدخول بحساب IPC.\tDer Zugriff erfolgt über eine kurzlebige sichere Sitzung; die Anmeldung verwendet weiterhin das IPC-Konto.\tL’accès utilise une session sécurisée de courte durée ; la connexion utilise toujours le compte IPC.\t短期の安全なセッションでアクセスし、ログインには引き続き IPC のアカウントを使用します。\tO acesso usa uma sessão segura de curta duração; o login continua usando a conta do IPC.\tДоступ выполняется через краткосрочный защищённый сеанс; вход по-прежнему использует учётную запись IPC.\t단기 보안 세션으로 접근하며 로그인에는 계속 IPC 계정을 사용합니다.\tAkses menggunakan sesi aman jangka pendek; login tetap memakai akun IPC.\tErişim kısa süreli güvenli oturum kullanır; giriş için IPC hesabı kullanılmaya devam eder.
浏览器已拦截新窗口，请允许本站打开弹窗后重试\tThe browser blocked the new window. Allow pop-ups for this site and try again.\tEl navegador bloqueó la ventana nueva. Permite ventanas emergentes para este sitio e inténtalo de nuevo.\tحظر المتصفح النافذة الجديدة. اسمح بالنوافذ المنبثقة لهذا الموقع ثم أعد المحاولة.\tDer Browser hat das neue Fenster blockiert. Erlauben Sie Pop-ups für diese Website und versuchen Sie es erneut.\tLe navigateur a bloqué la nouvelle fenêtre. Autorisez les fenêtres contextuelles pour ce site, puis réessayez.\tブラウザーが新しいウィンドウをブロックしました。このサイトのポップアップを許可して再試行してください。\tO navegador bloqueou a nova janela. Permita pop-ups para este site e tente novamente.\tБраузер заблокировал новое окно. Разрешите всплывающие окна для этого сайта и повторите попытку.\t브라우저가 새 창을 차단했습니다. 이 사이트의 팝업을 허용한 후 다시 시도하세요.\tBrowser memblokir jendela baru. Izinkan pop-up untuk situs ini lalu coba lagi.\tTarayıcı yeni pencereyi engelledi. Bu site için açılır pencerelere izin verip tekrar deneyin.
打开管理后台\tOpen administration\tAbrir administración\tفتح الإدارة\tVerwaltung öffnen\tOuvrir l’administration\t管理画面を開く\tAbrir administração\tОткрыть администрирование\t관리 화면 열기\tBuka administrasi\tYönetimi aç
WVP ZLM 可用\tWVP ZLM available\tWVP ZLM disponible\tWVP ZLM متاح\tWVP ZLM verfügbar\tWVP ZLM disponible\tWVP ZLM 利用可能\tWVP ZLM disponível\tWVP ZLM доступен\tWVP ZLM 사용 가능\tWVP ZLM tersedia\tWVP ZLM kullanılabilir
WVP ZLM 不可用\tWVP ZLM unavailable\tWVP ZLM no disponible\tWVP ZLM غير متاح\tWVP ZLM nicht verfügbar\tWVP ZLM indisponible\tWVP ZLM 利用不可\tWVP ZLM indisponível\tWVP ZLM недоступен\tWVP ZLM 사용 불가\tWVP ZLM tidak tersedia\tWVP ZLM kullanılamıyor
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
