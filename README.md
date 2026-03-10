# Reactive Spring Boot Boilerplate (JWT & WebFlux)

Boilerplate ini dirancang untuk performa tinggi menggunakan stack **Non-Blocking** dan arsitektur **Functional Routing**. Fokus utama adalah memisahkan antara definisi endpoint dan logika eksekusi tanpa menggunakan `@RestController` tradisional.

---

## 🏗️ Technical Architecture

Arsitektur project ini memisahkan tanggung jawab ke dalam layer-layer reaktif berikut:

### 1. Delivery Layer (API)
Terletak di package `com.jasavast.api`. Menggunakan pola fungsional:
* **Router**: (Contoh: `CobaAja.java`) Menggunakan `RouterFunction` untuk mendefinisikan mapping HTTP request.
* **Handler**: Mengolah `ServerRequest` dan mengembalikan `Mono<ServerResponse>`.

### 2. Security Layer
Terletak di package `com.jasavast.security`. Implementasi keamanan bersifat **Stateless**:
* **AuthenticationManager**: Memproses autentikasi berbasis token.
* **SecurityContextRepository**: Menyimpan context secara asinkron.
* **PBKDF2Encoder**: Hashing password yang kompatibel dengan standar keamanan tinggi.

### 3. Model & Repository
* **Entity & DTO**: Berada di package `model`.
* **R2DBC**: Akses database asinkron menggunakan Spring Data Reactive.

---

## 🛡️ Security Implementation

Project ini menggunakan JWT dengan alur sebagai berikut:
1. Client melakukan login via endpoint auth.
2. Server memvalidasi password via `PBKDF2Encoder`.
3. Server mengirimkan JWT yang berisi `roles` dan `claims`.
4. Setiap request berikutnya divalidasi oleh `AuthenticationManager` melalui header `Authorization: Bearer <token>`.

---

## 🛠️ Development Guide

### Standar Penulisan Endpoint
Gunakan pola fungsional seperti di bawah ini untuk menjaga konsistensi codebase:

```java
// Definisi Router
@Bean
public RouterFunction<ServerResponse> productRoute(ProductHandler handler) {
    return RouterFunctions.route()
        .GET("/api/v1/products", handler::findAll)
        .POST("/api/v1/products", accept(APPLICATION_JSON), handler::save)
        .build();
}

// Implementasi Handler
public Mono<ServerResponse> findAll(ServerRequest request) {
    return ServerResponse.ok()
        .contentType(MediaType.APPLICATION_JSON)
        .body(service.getAll(), Product.class);
}
