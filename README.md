# Reactive Spring Boot Boilerplate (WebFlux, MongoDB, JWT)

Boilerplate non-blocking: **Spring Boot 4 / WebFlux**, **reactive MongoDB**, autentikasi **JWT stateless**,
dan satu endpoint **RPC-style** (`/api/request`) yang mendispatch request ke bean berdasarkan nama.

| Komponen | Pilihan |
|---|---|
| Runtime | Java 21, Spring Boot 4.1, Spring Security 7, Reactor |
| Persistence | Spring Data MongoDB Reactive |
| Token | jjwt 0.13, HS512 |
| Error | RFC 9457 `ProblemDetail` (bawaan Spring) |
| Dokumentasi | springdoc-openapi (`/swagger-ui.html`) |

---

## Konsep: dispatcher `/api/request`

Endpoint bisnis tidak didefinisikan satu per satu di controller. Cukup buat bean turunan
`ApiAbstract` dan beri anotasi per HTTP verb:

```java
@Component("apiProduk")
public class ProdukApi extends ApiAbstract {

    @GetExecution                       // GET /api/request?method=apiProduk.cari&nama=kopi
    public Mono<ApiResponse<ObjectNode>> cari(ReqVM request) {
        String nama = request.getData().path("nama").asString();
        return produkService.cari(nama).map(this::success);
    }

    @PostExecution(id = "simpan")       // POST /api/request {"method":"apiProduk.simpan","data":{...}}
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public Mono<ApiResponse<ObjectNode>> create(ReqVM request) { ... }
}
```

Aturan (divalidasi saat startup oleh `ApiDispatcher`; signature salah = aplikasi gagal start):

* Nama operasi = `<nama bean>.<id anotasi | nama method>`.
* Signature wajib: `public Mono<?> nama(ReqVM request)`.
* Hanya bean turunan `ApiAbstract` yang bisa dipanggil (bukan sembarang bean di context).
* Verb harus cocok dengan anotasi (`@GetExecution`, `@PostExecution`, `@PutExecution`, `@DeleteExecution`),
  kalau tidak → `405`. Operasi tidak dikenal → `404`.
* GET/DELETE: query parameter selain `method` menjadi `data` (string). POST/PUT: body `{"method","data"}`.
* **Bean adalah singleton yang dipakai bersamaan oleh banyak request** — data request hanya boleh
  lewat parameter, jangan disimpan di field.
* `@PreAuthorize` berlaku per operasi (method security aktif); semua `/api/request` minimal harus terautentikasi.

Setiap eksekusi operasi dicatat oleh `AspectConfiguration` ke koleksi `audit_events`
(principal, class, method, durasi, error). Penulisan log bersifat fire-and-forget: tidak menambah
latency dan tidak menggagalkan request, dengan konsekuensi entri bisa hilang bila proses mati.
Payload request sengaja tidak disimpan (bisa berisi kredensial/PII).

## Akun & autentikasi

| Endpoint | Akses | Keterangan |
|---|---|---|
| `POST /api/register` | publik | buat akun **non-aktif**, kirim email aktivasi |
| `GET /api/activate?key=` | publik | aktivasi akun |
| `POST /api/login` | publik | `{"login","password","rememberMe"}` → `{"id_token"}` + header `Authorization` |
| `GET /api/account` | login | data user saat ini |
| `POST /api/forgot-password` | publik | `{"email"}` atau `{"login"}`; respons selalu sama (anti user-enumeration) |
| `POST /api/reset-password` | publik | `{"key","newPassword"}`, key berlaku 24 jam |

Request berikutnya: `Authorization: Bearer <token>`.

Catatan desain:

* Password di-hash BCrypt, dijalankan di `boundedElastic` (bukan di event loop).
* Key aktivasi/reset disimpan sebagai SHA-256, bukan plaintext.
* `login` dan `email` disimpan lowercase dengan unique index — index inilah penjaga sebenarnya
  untuk registrasi bersamaan; pengecekan di service hanya untuk pesan error yang ramah.
* JWT stateless **tanpa revocation**: logout = client membuang token. Atur masa berlaku sesuai kebutuhan.

## Menjalankan

```bash
docker run -d --name mongo -p 27017:27017 mongo:8
```

```bash
./mvnw spring-boot:run
```

Tanpa profile eksplisit, profile `dev` dipakai (Mongo `localhost:27017/boiler-dev`, secret dev, CORS untuk
`localhost:3000`/`4200`, log DEBUG, Reactor debug hook). Bila `spring.mail.host` tidak di-set, email tidak dikirim
dan link aktivasi tercatat di log DEBUG.

Swagger UI: http://localhost:8080/swagger-ui.html · Health: `/actuator/health`

### Konfigurasi production (`--spring.profiles.active=prod`)

| Variabel | Wajib | Keterangan |
|---|---|---|
| `JWT_SECRET` | ya | Base64, ≥ 64 byte: `openssl rand -base64 64`. Aplikasi gagal start bila kosong/lemah. |
| `MONGODB_URI` | ya | connection string MongoDB |
| `APP_BASE_URL` | ya | base URL publik untuk link di email |
| `MAIL_FROM`, `SPRING_MAIL_HOST`, ... | untuk email | properti standar `spring.mail.*` |
| `APPLICATION_CORS_ALLOWED_ORIGINS` | opsional | daftar origin (dipisah koma); default tidak ada CORS |

Profile `prod` mematikan Swagger/OpenAPI. Endpoint actuator selain health/info butuh `ROLE_ADMIN`.

## Struktur package

```
com.jasavast
├── api/            bean operasi (contoh: api/coba/CobaAja)
├── config/         security, CORS, MongoDB (auditing + seed authority), OpenAPI, properties
├── core/
│   ├── annotation/ @Get/@Post/@Put/@DeleteExecution + aspect audit
│   ├── api/        ApiDispatcher, ApiResponse
│   ├── error/      exception bisnis + ExceptionTranslator (ProblemDetail)
│   ├── security/   TokenProvider, JWTFilter, SecurityUserService, SecurityUtils
│   └── util/
├── domain/         dokumen MongoDB
├── repository/
├── resource/       CoreApi (/api/request), AccountResource
└── service/        ApiAbstract, AccountService, MailService
```

## Test

```bash
./mvnw test
```

Test memakai `@WebFluxTest` dengan security, dispatcher, aspect, dan service asli; hanya repository yang di-mock.
Belum ada integration test terhadap MongoDB sungguhan (unique index, auditing) — tambahkan dengan Testcontainers
bila CI menyediakan Docker.

## Belum termasuk (sengaja)

* Retensi `audit_events` (TTL index / capped collection) — tentukan sesuai kebijakan retensi.
* Rate limiting untuk `/api/login` dan `/api/forgot-password`.
* Refresh token / revocation.
