# Reactive Spring Boot Boilerplate (WebFlux, R2DBC PostgreSQL, JWT)

Boilerplate non-blocking: **Spring Boot 4 / WebFlux**, **R2DBC PostgreSQL**, autentikasi **JWT stateless**,
dan satu controller generik yang mendispatch `/api/{module}/{subModule}/{action}` ke bean berdasarkan nama.

| Komponen | Pilihan |
|---|---|
| Runtime | Java 21, Spring Boot 4.1, Spring Security 7, Reactor |
| Persistence | Spring Data R2DBC + r2dbc-postgresql (runtime), Flyway via JDBC (migrasi saat startup) |
| Token | jjwt 0.13, HS512 |
| Error | RFC 9457 `ProblemDetail` (bawaan Spring) |
| Dokumentasi | springdoc-openapi (`/swagger-ui.html`) |

---

## Konsep: dispatcher `/api/{module}/{subModule}/{action}`

Endpoint bisnis tidak didefinisikan satu per satu di controller. Buat bean turunan `ApiAbstract` bernama
`api<Module><SubModule>` dan beri anotasi per HTTP verb:

```java
@Component("apiProdukKatalog")                       // → /api/produk/katalog/...
public class ProdukKatalogApi extends ApiAbstract {

    @GetExecution                                    // GET /api/produk/katalog/cari?nama=kopi
    public Mono<ApiResponse<List<Produk>>> cari(ApiRequest request) {
        String nama = request.data().path("nama").asString();
        return db().sql("SELECT id, nama, harga FROM produk WHERE nama ILIKE :nama LIMIT 50")
                .bind("nama", "%" + nama + "%")
                .map((row, meta) -> new Produk(row.get("id", Long.class), row.get("nama", String.class),
                        row.get("harga", BigDecimal.class)))
                .all().collectList()
                .map(this::success);
    }

    @PostExecution(id = "simpan")                    // POST /api/produk/katalog/simpan  {...}
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public Mono<ApiResponse<Produk>> create(ApiRequest request) { ... }
}
```

Aturan (divalidasi saat startup oleh `ApiDispatcher`; pelanggaran = aplikasi gagal start):

* Bean dicari dengan `"api" + module + subModule` (**case-insensitive**); nama action case-sensitive
  (`id()` anotasi bila diisi, selain itu nama method).
* Signature wajib: `public Mono<?> action(ApiRequest request)`.
* Hanya bean turunan `ApiAbstract` yang bisa dipanggil, dan namanya wajib berawalan `api`.
* Verb harus cocok dengan anotasi (`@GetExecution`, `@PostExecution`, `@PutExecution`, `@DeleteExecution`),
  kalau tidak → `405`. Operasi tidak dikenal → `404`.
* `ApiRequest.data()`: body JSON untuk POST/PUT (objek kosong bila tanpa body); query parameter (string) untuk
  GET/DELETE. `queryParams()` dan `exchange()` (header/status response) tersedia untuk semua verb.
* **Bean adalah singleton yang dipakai bersamaan oleh banyak request** — data request hanya boleh lewat
  parameter, jangan disimpan di field.
* `ApiAbstract` menyediakan `template()` (`R2dbcEntityTemplate`) dan `db()` (`DatabaseClient`) untuk SQL eksplisit.
* `@PreAuthorize` berlaku per operasi; semua `/api/{m}/{s}/{a}` minimal harus terautentikasi.

Setiap eksekusi operasi dicatat oleh `AspectConfiguration` ke tabel `jvs_audit` (principal, class, method,
durasi, error). Penulisan bersifat fire-and-forget: tidak menambah latency dan tidak menggagalkan request,
dengan konsekuensi entri bisa hilang bila proses mati. Payload request sengaja tidak disimpan.

## Akun & autentikasi

| Endpoint | Akses | Keterangan |
|---|---|---|
| `POST /api/register` | publik | buat akun **non-aktif**, kirim email aktivasi |
| `GET /api/activate?key=` | publik | aktivasi akun |
| `POST /api/login` | publik | `{"login","password","rememberMe"}` → `{"id_token"}` + header `Authorization` |
| `GET /api/account` | login | data user saat ini |
| `POST /api/forgot-password` | publik | `{"email"}` atau `{"login"}`; respons selalu sama (anti user-enumeration) |
| `POST /api/reset-password` | publik | `{"key","newPassword"}`, key berlaku 24 jam |

Catatan desain:

* Login: satu query (`users` LEFT JOIN `user_authority`, authority di-`array_agg`).
* Registrasi dalam satu transaksi (`TransactionalOperator`): hapus registrasi **non-aktif** dengan login/email
  sama → insert user → insert role. Akun aktif dilindungi unique constraint (`ux_users_login`, `ux_users_email`)
  → `409`. BCrypt dijalankan sebelum transaksi dan email dikirim setelah commit, jadi koneksi tidak tertahan.
* Aktivasi dan reset password adalah satu `UPDATE ... WHERE key_hash = ? AND ...` bersyarat: key tidak bisa
  dipakai dua kali walau request bersamaan.
* Key aktivasi/reset disimpan sebagai SHA-256, bukan plaintext.
* Konsekuensi semantik "registrasi non-aktif boleh ditimpa": siapa pun yang mendaftar ulang dengan login/email
  yang sama sebelum aktivasi akan menggantikan registrasi sebelumnya.
* JWT stateless **tanpa revocation**: logout = client membuang token.

## Schema

Dikelola Flyway: `src/main/resources/db/migration` (`V1__init.sql` membuat `authority` + seed role, `users`,
`user_authority`, `jvs_audit`). Perubahan schema = file migrasi baru (`V2__...sql`), jangan ubah file lama.

R2DBC tidak punya relasi ala JPA: relasi dibaca dengan query eksplisit di `UserRepository`.

## Menjalankan

```bash
docker run -d --name pg -p 5432:5432 -e POSTGRES_PASSWORD=postgres -e POSTGRES_DB=coba postgres:16
```

```bash
./mvnw spring-boot:run
```

Tanpa profile eksplisit, profile `dev` dipakai (DB `localhost:5432/coba`, user/password `postgres`, secret dev,
CORS untuk `localhost:3000`/`4200`, log DEBUG, Reactor debug hook). Bila `spring.mail.host` tidak di-set,
email tidak dikirim dan link aktivasi/reset tercatat di log DEBUG.

Swagger UI: http://localhost:8080/swagger-ui.html · Health: `/actuator/health`

### Konfigurasi production (`--spring.profiles.active=prod`)

| Variabel | Wajib | Keterangan |
|---|---|---|
| `DB_HOST`, `DB_PORT` (5432), `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD` | ya | dipakai R2DBC dan Flyway |
| `JWT_SECRET` | ya | Base64, ≥ 64 byte: `openssl rand -base64 64`. Aplikasi gagal start bila kosong/lemah. |
| `APP_BASE_URL` | ya | base URL publik untuk link di email |
| `MAIL_FROM`, `SPRING_MAIL_HOST`, ... | untuk email | properti standar `spring.mail.*` |
| `APPLICATION_CORS_ALLOWED_ORIGINS` | opsional | daftar origin (dipisah koma); default tidak ada CORS |

Profile `prod` mematikan Swagger/OpenAPI. User DB untuk Flyway butuh hak DDL; bila ingin memisahkan, set
`SPRING_FLYWAY_USER`/`SPRING_FLYWAY_PASSWORD` berbeda dari user runtime.

## Test

```bash
./mvnw test
```

* Unit + `@WebFluxTest` (security, dispatcher, aspect, service asli; repository di-mock).
* `PostgresIntegrationTest`: aplikasi penuh terhadap PostgreSQL sungguhan (migrasi, SQL, constraint,
  transaksi, auditing, registrasi bersamaan). Sumber DB: `IT_DB_HOST` (+ `IT_DB_PORT`, `IT_DB_NAME`,
  `IT_DB_USERNAME`, `IT_DB_PASSWORD`), atau Testcontainers bila Docker tersedia; selain itu di-skip.

```bash
IT_DB_HOST=localhost IT_DB_NAME=coba_it ./mvnw test
```

## Belum termasuk (sengaja)

* Retensi `jvs_audit` (partisi / purge terjadwal) — tentukan sesuai kebijakan retensi.
* Rate limiting untuk `/api/login` dan `/api/forgot-password`.
* Refresh token / revocation.
* Redis: dependency di branch lama tidak dipakai, jadi dihapus. Tambahkan bila ada kebutuhan cache yang jelas
  (beserta strategi invalidasi).
