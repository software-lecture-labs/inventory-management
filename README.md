# Envanter Yönetim Sistemi

Laboratuvar, ofis veya atölye gibi ortamlarda **ekipmanların takibi**, **zaman bazlı rezervasyonu**,
**teslim alma / iade süreci** ve **işlem geçmişinin raporlanması** için geliştirilmiş tam yığın (full-stack)
bir web uygulaması.

## Özellikler

### Ekipman
| Özellik | Açıklama |
|---|---|
| Ekipman yönetimi | Ekleme, düzenleme, silme (kayıt silinmez, pasife alınır); ad, marka, model veya seri no ile arama |
| Durum | Kullanılabilir / Arızalı / Bakımda / Kalibrasyonda. Kullanılabilir olmayan ekipman rezerve edilemez ve teslim alınamaz |
| Kategori ve konum | Her ekipmana kategori ve konum (ör. "Lab 2 / Raf A"); listede bunlara göre filtreleme |
| Bakım ve kalibrasyon geçmişi | Ekipman detay sayfasında bakım / kalibrasyon / onarım kayıtları. Kalibrasyon kaydı ekipmanın kalibrasyon tarihini günceller |
| Kalibrasyon uyarısı | Kalibrasyon tarihi geçmiş veya 30 gün içinde olan ekipmanlar listede işaretlenir |
| Excel içe / dışa aktarma | Ekipman listesini .xlsx olarak indirme, şablonla toplu ekleme; hatalı satırlar satır numarasıyla raporlanır |

### Rezervasyon
| Özellik | Açıklama |
|---|---|
| Takvim | Aylık / haftalık / günlük görünüm, 24 saat; boş aralığı sürükleyerek rezervasyon; çakışma kontrolü |
| Aranabilir ekipman seçimi | Rezervasyon formunda ekipman ad veya seri no yazılarak bulunur |
| Onay akışı | "Onay gerekli" işaretli ekipmanlarda kullanıcı rezervasyonları yönetici onaylayana kadar bekler; yönetici gerekçe yazarak reddedebilir |
| Teslim alma / iade | Ekipman başlangıçtan en erken 30 dk önce teslim alınır; iade sırasında arıza bildirilirse ekipman "Arızalı" olur; gecikmiş ve geç iadeler işaretlenir |
| Bekleme listesi | Dolu bir aralık için sıraya girilir. Aralık iptal, red veya erken iade ile boşalınca ilk sıradakine bildirim gider ve listeden çıkarılır; bildirimdeki "Rezerve Et" ile form dolu açılır. Beklenen aralıkla çakışan bir rezervasyon yapılırsa da listeden çıkılır |

### Bildirimler
Üst bardaki **zil** simgesi okunmamış bildirim sayısını gösterir (30 saniyede bir yenilenir).

| Kime | Ne zaman |
|---|---|
| Kullanıcı | Bekleme listesindeki aralık boşaldığında; rezervasyonu onaylandığında, reddedildiğinde veya yönetici tarafından iptal edildiğinde; başlangıçtan 1 saat önce; iade süresi geçtiğinde |
| Yönetici | Onay bekleyen yeni bir talep geldiğinde; "Şifremi unuttum" talebi geldiğinde; her sabah kalibrasyonu yaklaşan ekipman varsa |

### Raporlama
| Özellik | Açıklama |
|---|---|
| Özet paneli | Ekipman durum dağılımı, son 14 günün rezervasyon grafiği, en çok kullanılan ekipmanlar, kalibrasyonu yaklaşanlar, yaklaşan rezervasyonlarım; yöneticiye onay bekleyen ve gecikmiş iade sayıları |
| İşlem geçmişi (audit log) | Ekipman, rezervasyon, kullanıcı ve bakım işlemleri kim tarafından, ne zaman yapıldığıyla kaydedilir; güncellemelerde alan bazında eski → yeni değer tutulur |
| Raporlar | Filtrelenebilir, sayfalı işlem geçmişi ve rezervasyon raporu |
| PDF | Rezervasyon raporu, işlem geçmişi ve ekipman envanteri, ekrandaki filtrelerle PDF olarak indirilebilir |

### Kullanıcı ve güvenlik
| Özellik | Açıklama |
|---|---|
| Kayıt / giriş | JWT tabanlı kimlik doğrulama, BCrypt ile saklanan şifreler |
| Roller | `USER` ve `ADMIN`; yetkiler backend'de kontrol edilir |
| Giriş denemesi sınırı | Art arda 5 hatalı girişte hesap 15 dakika kilitlenir; yönetici kilidi açabilir |
| Engelleme | Yönetici bir kullanıcıyı engelleyebilir: giriş yapamaz, açık oturumu kapanır, ileri tarihli rezervasyonları iptal edilir |
| Şifremi unuttum | Talep yöneticiye düşer; yönetici 24 saat geçerli tek kullanımlık şifre verir; kullanıcı bu şifreyle girince yeni şifresini belirlemeden devam edemez |

### Teknik
- Tüm listelerde sunucu taraflı **sayfalama** ve filtreleme
- Takvim sadece ekranda görünen tarih aralığını yükler

## Kim Ne Yapabilir?

| İşlem | Kullanıcı | Yönetici |
|---|:---:|:---:|
| Ekipmanları görme, rezervasyon yapma, bekleme listesine girme | ✓ | ✓ |
| Kendi rezervasyonunu iptal etme, teslim alma, iade etme | ✓ | ✓ |
| Başkasının rezervasyonunu iptal etme / teslim alma / iade etme | | ✓ |
| Ekipman ekleme, düzenleme, silme, durum değiştirme, bakım kaydı | | ✓ |
| Excel içe / dışa aktarma, PDF raporlar | | ✓ |
| Rezervasyon onaylama / reddetme | | ✓ |
| Kullanıcı rolü değiştirme, engelleme, tek kullanımlık şifre verme | | ✓ |
| İşlem geçmişi ve raporlar | | ✓ |

## İş Akışları

### Rezervasyon yaşam döngüsü

```
                 ┌──────────── onay gerekmiyorsa ────────────┐
Oluşturuldu ─────┤                                            ▼
                 └─▶ PENDING (onay bekliyor) ──onay──▶ APPROVED ──teslim al──▶ CHECKED_OUT ──iade et──▶ RETURNED
                          │                              │
                         red                           iptal
                          ▼                              ▼
                      REJECTED                       CANCELLED
```

- Takvimde yer kaplayan durumlar: `PENDING`, `APPROVED`, `CHECKED_OUT`. Çakışma kontrolü bunlara göre yapılır.
- Reddedilen, iptal edilen veya bitiş zamanından önce iade edilen rezervasyonun boşalttığı aralık
  için bekleme listesindekilere bildirim gider.
- Teslim alınmış bir rezervasyon iptal edilemez; önce iade edilmelidir.

### Şifre sıfırlama

1. Kullanıcı giriş sayfasındaki **"Şifremi unuttum"** bağlantısından e-posta adresini yazar.
   Kayıtlı olmayan bir adres yazılsa da aynı cevap döner (hangi adreslerin kayıtlı olduğu dışarıdan öğrenilemez).
2. Yöneticilere bildirim gider; talep **Kullanıcılar → Şifre Talepleri** sekmesinde görünür.
3. Yönetici **tek kullanımlık şifre** belirler (otomatik oluşturulabilir) ve kullanıcıya iletir.
   Kullanıcının eski şifresi o anda geçersiz olur. Tek kullanımlık şifre 24 saat geçerlidir.
4. Kullanıcı bu şifreyle giriş yapınca **yeni şifre belirleme** ekranı açılır. Yeni şifreyi belirleyene kadar
   uygulamada başka bir işlem yapamaz; bu kural backend'de de uygulanır.

Yönetici, talep olmadan da herhangi bir kullanıcıya Kullanıcılar sayfasındaki 🔑 düğmesiyle tek kullanımlık şifre verebilir.

### Zamanlanmış işler

| Ne zaman | İş |
|---|---|
| 5 dakikada bir | 1 saat içinde başlayacak rezervasyonlar için hatırlatma; iade süresi geçenler için uyarı (her rezervasyon için bir kez) |
| Her sabah 08:00 | Kalibrasyonuna 7 gün kalan / geçmiş ekipmanları yöneticilere bildirme; süresi geçmiş bekleme kayıtlarını ve 30 günden eski okunmuş bildirimleri silme |

## Kullanılan Teknolojiler

**Backend:** Java 17, Spring Boot 3.5, Spring Web, Spring Data JPA (Hibernate), Spring Security, JWT (jjwt), Bean Validation, Apache POI (Excel), OpenPDF (PDF)
**Veritabanı:** PostgreSQL (testlerde H2)
**Frontend:** React 18, Vite, React Router, React Bootstrap, Axios, FullCalendar, Recharts
**Test:** JUnit 5, Mockito, Spring Boot Test (MockMvc)

## Mimari

```
┌──────────────┐   HTTP + JSON (JWT)   ┌───────────────────────────────────────────┐      ┌────────────┐
│  React (SPA) │ ────────────────────▶ │ Controller → Service → Repository (JPA)   │ ───▶ │ PostgreSQL │
└──────────────┘                       │      ▲ Security Filter (JWT doğrulama)    │      └────────────┘
                                       └───────────────────────────────────────────┘
```

Backend katmanlı mimariyle yazılmıştır:

- **controller** — HTTP isteklerini karşılar, sadece servisleri çağırır
- **service** — İş kuralları (çakışma kontrolü, onay, bekleme listesi, yetki, audit log, bildirimler, Excel / PDF)
- **repository** — Spring Data JPA arayüzleri
- **model** — Veritabanı tabloları (entity)
- **dto** — API'nin dışarıya açtığı istek/cevap modelleri (entity'ler doğrudan dışarı verilmez)
- **security** — JWT üretimi ve doğrulanması, zorunlu şifre değişikliği filtresi
- **exception** — Tüm hataları tek formatta (`{ status, message, fieldErrors }`) döndüren merkezi hata yönetimi

### Veritabanı tabloları

| Tablo | İçerik |
|---|---|
| `users` | Kullanıcılar (ad, soyad, email, telefon, şifre hash'i, rol, engel / kilit / şifre değiştirme durumu) |
| `equipment` | Ekipmanlar (`active = false` ise silinmiş sayılır) |
| `reservations` | Ekipman + kullanıcı + zaman aralığı + durum, teslim / iade zamanları |
| `waitlist_entries` | Dolu aralıklar için bekleme listesi |
| `maintenance_records` | Bakım, kalibrasyon ve onarım geçmişi |
| `notifications` | Uygulama içi bildirimler |
| `password_reset_requests` | "Şifremi unuttum" talepleri |
| `audit_logs` | Ekipman, rezervasyon, kullanıcı ve bakım işlemleri |
| `audit_log_changes` | Güncelleme işlemlerinde değişen alanlar (eski / yeni değer) |

Tablolar uygulama ilk açıldığında Hibernate tarafından otomatik oluşturulur. Önceki bir sürümle oluşturulmuş
veritabanı da açılışta otomatik yükseltilir: yeni kolonlar varsayılan değerleriyle eklenir ve yeni değer alan
enum kolonlarının eski CHECK kısıtları kaldırılır (`config/SchemaUpgrade.java`).

## Proje Yapısı

```
├── backend/                         Spring Boot uygulaması
│   ├── pom.xml
│   └── src/
│       ├── main/java/com/inventory/
│       │   ├── config/              Güvenlik, CORS, ilk yönetici hesabı, şema yükseltme
│       │   ├── controller/
│       │   ├── service/
│       │   ├── repository/
│       │   ├── model/
│       │   ├── dto/
│       │   ├── security/
│       │   └── exception/
│       └── test/                    Birim ve entegrasyon testleri
├── frontend/                        React uygulaması
│   └── src/
│       ├── api/                     Axios istemcisi (token ekleme, hata mesajları, dosya indirme)
│       ├── context/                 Oturum ve bildirim (toast) yönetimi
│       ├── hooks/                   Sayfalı liste yükleme
│       ├── components/              Ortak bileşenler (layout, formlar, modallar, bildirim zili)
│       ├── pages/                   Sayfalar
│       ├── styles/
│       └── utils/                   Tarih biçimleri ve durum etiketleri
└── docker-compose.yml               PostgreSQL (isteğe bağlı)
```

## Kurulum ve Çalıştırma

### Gereksinimler

- Java 17+
- Node.js 18+
- PostgreSQL 14+ (veya Docker)

### 1. Veritabanı

**Seçenek A — Docker ile:**

```bash
docker compose up -d
```

**Seçenek B — Kurulu PostgreSQL ile:** `inventory_db` adında boş bir veritabanı oluşturun:

```sql
CREATE DATABASE inventory_db;
```

### 2. Backend

`backend` klasöründe bir `.env` dosyası oluşturup veritabanı bilgilerini yazın. Bu dosya git'e gönderilmez!

> Docker ile çalıştırıyorsanız kullanıcı adı ve şifre `docker-compose.yml` içindeki gibi `postgres / postgres` olmalıdır.

Ardından:

```bash
cd backend
./mvnw spring-boot:run        # Windows: .\mvnw.cmd spring-boot:run
```

Backend `http://localhost:8080` adresinde çalışır.

`.env` dosyasında (veya ortam değişkeni olarak) kullanılabilecek değerler:

| Değişken | Zorunlu | Açıklama |
|---|---|---|
| `DB_HOST` | Evet | Veritabanı sunucusu |
| `DB_PORT` | Evet | Veritabanı portu |
| `DB_NAME` | Evet | Veritabanı adı |
| `DB_USERNAME` | Evet | Veritabanı kullanıcısı |
| `DB_PASSWORD` | Evet | Veritabanı şifresi |
| `JWT_SECRET` | Hayır (geliştirme anahtarı) | Base64, en az 256 bit. Üretim ortamında mutlaka değiştirin |
| `JWT_EXPIRATION_MINUTES` | Hayır (`480`) | Oturum süresi (dakika) |
| `CORS_ALLOWED_ORIGINS` | Hayır (`http://localhost:5173`) | Frontend'in adresi |
| `ADMIN_EMAIL` | Hayır (`admin@envanter.com`) | İlk yönetici hesabı |
| `ADMIN_PASSWORD` | Hayır (`Admin123!`) | İlk yönetici şifresi |
| `MAX_LOGIN_ATTEMPTS` | Hayır (`5`) | Hesap kilitlenmeden önceki hatalı deneme hakkı |
| `LOGIN_LOCK_MINUTES` | Hayır (`15`) | Kilit süresi (dakika) |

Diğer ayarlar (`backend/src/main/resources/application.properties`):

| Ayar | Varsayılan | Açıklama |
|---|---|---|
| `app.security.temporary-password-hours` | `24` | Tek kullanımlık şifrenin geçerlilik süresi |
| `app.reservation.check-out-early-minutes` | `30` | Başlangıçtan en fazla kaç dakika önce teslim alınabilir |
| `app.reservation.reminder-minutes` | `60` | Başlangıçtan kaç dakika önce hatırlatma bildirimi gönderilir |
| `app.calibration.warning-days` | `7` | Kalibrasyonuna kaç gün kalan ekipman günlük bildirime girer |
| `app.scheduler.enabled` | `true` | Zamanlanmış işleri açar / kapatır |

### 3. Frontend

```bash
cd frontend
npm install
npm run dev
```

Uygulama `http://localhost:5173` adresinde açılır. Geliştirme sunucusu `/api` isteklerini otomatik olarak
backend'e yönlendirir. Backend farklı bir adresteyse `frontend/.env` dosyasına `VITE_API_URL` yazılabilir.

### 4. Giriş

Uygulama ilk açıldığında sistemde hiç yönetici yoksa otomatik olarak bir yönetici hesabı oluşturulur:

- **Email:** `admin@envanter.com`
- **Şifre:** `Admin123!`

Kayıt sayfasından oluşturulan hesaplar `USER` rolüyle başlar. Yönetici, **Kullanıcılar** sayfasından
başka bir kullanıcıyı yönetici yapabilir.

## Testler

```bash
cd backend
./mvnw test                   # Windows: .\mvnw.cmd test
```

Testler PostgreSQL gerektirmez, bellek içi H2 veritabanı kullanır:

- `ReservationServiceTest` — tarih ve çakışma kontrolü, onay akışı, iptal yetkisi, teslim alma zaman aralığı,
  geç iade ve arıza bildirimi, bekleme listesi bildirimi ve listeden çıkarma
- `EquipmentServiceDiffTest` — güncellemede değişen alanların tespiti
- `PdfReportServiceTest` — PDF'te Türkçe karakterlerin doğru çıkması
- `ApiIntegrationTest` — kayıt, giriş, yetkisiz erişim (401/403), hesap kilitleme, engelleme, yönetici üzerinden
  şifre sıfırlama, sayfalama, Excel içe aktarma, bakım kaydı, onay ve bekleme listesi akışı, bildirimler,
  PDF raporları, özet paneli

## API Özeti

Korumalı adreslere istek atarken `Authorization: Bearer <token>` başlığı gönderilmelidir.
Sayfalı uç noktalar `{ content, page, size, totalElements, totalPages }` döner (`page` 0'dan başlar, `size` en fazla 100).

### Kimlik doğrulama
| Metot | Adres | Yetki | Açıklama |
|---|---|---|---|
| POST | `/api/auth/register` | Herkes | Kayıt ol |
| POST | `/api/auth/login` | Herkes | Giriş yap, JWT al |
| POST | `/api/auth/forgot-password` | Herkes | Yöneticiye şifre sıfırlama talebi gönder |
| GET | `/api/auth/me` | Giriş yapmış | Oturumdaki kullanıcı |
| POST | `/api/auth/change-password` | Giriş yapmış | Şifre değiştir (tek kullanımlık şifreyle girildiyse zorunlu) |

### Ekipman
| Metot | Adres | Yetki | Açıklama |
|---|---|---|---|
| GET | `/api/equipment?q=&category=&location=&status=&page=&size=` | Giriş yapmış | Sayfalı ekipman listesi |
| GET | `/api/equipment/options` | Giriş yapmış | Açılır listeler için tüm ekipmanlar |
| GET | `/api/equipment/filters` | Giriş yapmış | Mevcut kategori ve konumlar |
| GET | `/api/equipment/{id}` | Giriş yapmış | Ekipman detayı |
| POST / PUT / DELETE | `/api/equipment[/{id}]` | ADMIN | Ekipman ekle / güncelle / sil |
| PATCH | `/api/equipment/{id}/status` | ADMIN | Durum değiştir |
| GET / POST | `/api/equipment/{id}/maintenance` | Giriş yapmış / ADMIN | Bakım geçmişi / kayıt ekle |
| GET | `/api/equipment/export`, `/api/equipment/import-template` | ADMIN | Excel indir |
| POST | `/api/equipment/import` | ADMIN | Excel'den içe aktar (multipart `file`) |

### Rezervasyon ve bekleme listesi
| Metot | Adres | Yetki | Açıklama |
|---|---|---|---|
| GET | `/api/reservations?from=&to=` | Giriş yapmış | Takvim aralığındaki rezervasyonlar |
| GET | `/api/reservations/search?user=&equipment=&status=&from=&to=&overdue=&mine=&page=` | Giriş yapmış | Sayfalı liste (kullanıcı sadece kendininkini görür) |
| POST | `/api/reservations` | Giriş yapmış | Rezervasyon oluştur |
| POST | `/api/reservations/{id}/approve`, `/reject` | ADMIN | Onayla / reddet |
| POST | `/api/reservations/{id}/cancel` | Sahibi / ADMIN | İptal et |
| POST | `/api/reservations/{id}/check-out`, `/return` | Sahibi / ADMIN | Teslim al / iade et |
| GET / POST / DELETE | `/api/waitlist[/{id}]` | Giriş yapmış | Bekleme listem / listeye gir / çık |

### Bildirimler ve özet
| Metot | Adres | Yetki | Açıklama |
|---|---|---|---|
| GET | `/api/notifications?page=&size=` | Giriş yapmış | Bildirimlerim (en yeni önce) |
| GET | `/api/notifications/unread-count` | Giriş yapmış | Okunmamış bildirim sayısı |
| POST | `/api/notifications/{id}/read`, `/api/notifications/read-all` | Giriş yapmış | Okundu işaretle |
| GET | `/api/dashboard` | Giriş yapmış | Özet paneli verileri |

### Yönetim
| Metot | Adres | Yetki | Açıklama |
|---|---|---|---|
| GET | `/api/users?q=&role=&enabled=&page=` | ADMIN | Sayfalı kullanıcı listesi |
| PATCH | `/api/users/{id}/role`, `/api/users/{id}/enabled` | ADMIN | Rol değiştir / engelle - engeli kaldır |
| GET | `/api/users/password-requests` | ADMIN | Bekleyen şifre talepleri |
| POST | `/api/users/{id}/temporary-password` | ADMIN | Tek kullanımlık şifre belirle |
| POST | `/api/users/password-requests/{id}/dismiss` | ADMIN | Şifre talebini yok say |
| GET | `/api/audit-logs?entityType=&action=&q=&from=&to=&page=` | ADMIN | Sayfalı işlem geçmişi |
| GET | `/api/reports/reservations.pdf`, `/audit-logs.pdf`, `/equipment.pdf` | ADMIN | PDF raporlar (liste filtreleriyle aynı parametreler) |
