# Smart Code Plagiarism Detector

A professional-grade backend system that detects similarity between source code files using tokenization, rolling hash algorithms, and fingerprint-based analysis. Built with Java 17 and Spring Boot 3.5.

---

## Table of Contents

1. [What This Project Does](#what-this-project-does)
2. [How It Works](#how-it-works)
3. [Tech Stack](#tech-stack)
4. [Project Structure](#project-structure)
5. [Environment Setup](#environment-setup)
6. [Running the Application](#running-the-application)
7. [API Reference](#api-reference)
8. [Understanding the Output](#understanding-the-output)
9. [Algorithm Deep Dive](#algorithm-deep-dive)
10. [Future Scope](#future-scope)

---

## What This Project Does

This system takes two source code files as input and determines how similar they are to each other. It goes far beyond simple text comparison — it understands the *structure* of code and can detect plagiarism even when:

- Variable names have been changed (`int a` → `int x`)
- String values have been modified
- Comments have been added or removed
- Code has been partially reordered

The system returns a **similarity score from 0 to 1** (0 = completely different, 1 = identical) along with a human-readable verdict and a detailed report of exactly which segments matched.

**Example use case:** A university professor uploads 30 student submissions. The system compares each pair and flags which ones are suspiciously similar.

---

## How It Works

The detection pipeline has four stages:

```
Raw Code File
      ↓
1. TOKENIZATION      — Break code into tokens, strip comments
      ↓
2. NORMALIZATION     — Replace variable names with VAR_0, VAR_1...
      ↓
3. ANALYSIS          — Rabin-Karp hashing + Winnowing fingerprinting
      ↓
4. SCORING           — Blend Jaccard + Containment + Coverage scores
      ↓
Similarity Report (JSON)
```

### Stage 1 — Tokenization

The `CodeTokenizer` reads raw source code and breaks it into a stream of tokens. Each token is classified as a keyword, identifier, string literal, number, operator, or delimiter.

**Before tokenization:**
```java
// Calculate sum
public int add(int a, int b) {
    return a + b;
}
```

**After tokenization:**
```
[KEYWORD: public] [KEYWORD: int] [IDENTIFIER: add] [DELIMITER: (]
[KEYWORD: int] [IDENTIFIER: a] [DELIMITER: ,] [KEYWORD: int]
[IDENTIFIER: b] [DELIMITER: )] [DELIMITER: {] [KEYWORD: return]
[IDENTIFIER: a] [OPERATOR: +] [IDENTIFIER: b] [DELIMITER: ;] [DELIMITER: }]
```

Notice the comment `// Calculate sum` is completely removed.

### Stage 2 — Normalization

After tokenization, all user-defined identifiers are replaced with canonical names. This is the key step that catches renamed-variable plagiarism.

**Before normalization:**
```
public int add ( int a , int b ) { return a + b ; }
```

**After normalization:**
```
public int VAR_0 ( int VAR_1 , int VAR_2 ) { return VAR_1 + VAR_2 ; }
```

Now if another student writes `public int sum(int x, int y) { return x + y; }`, it normalizes to the exact same token stream — and gets caught.

### Stage 3 — Analysis (Two algorithms run in parallel)

**Rabin-Karp Rolling Hash** finds *where* the copied segments are. It slides a window of k tokens across both files and computes a hash for each window. Matching hashes indicate a copied segment. The algorithm then tries to extend each match as far as possible to find the full length of the copied block.

**Winnowing Fingerprinting** measures *how similar* the two files are overall. It selects a canonical subset of k-gram hashes from each file (the "fingerprint") and compares them using set mathematics. This is the same algorithm used by Stanford's MOSS system.

### Stage 4 — Scoring

Three scores are computed and blended:

| Score | What it measures | Weight |
|---|---|---|
| Jaccard | Overlap of fingerprint sets | 35% |
| Weighted | Jaccard + containment (catches partial copies) | 35% |
| Coverage | What fraction of the shorter file is matched | 30% |

The **containment score** specifically catches the case where a student copies one method from a large file. Pure Jaccard would score this low because the large file contributes many unique tokens. Containment normalizes against the smaller file and correctly flags the overlap.

---

## Tech Stack

| Component | Technology | Version | Purpose |
|---|---|---|---|
| Language | Java | 17 LTS | Core application language |
| Framework | Spring Boot | 3.5.14 | REST API and dependency injection |
| Build Tool | Maven | 3.9+ | Dependency management and build |
| API Docs | SpringDoc OpenAPI | 2.5.0 | Auto-generates Swagger UI |
| Testing | JUnit 5 | (via Spring Boot) | Unit and integration tests |
| Server | Apache Tomcat | 10.1 (embedded) | HTTP server (bundled) |

**No database is required.** The system processes files in memory and returns results immediately.

---

## Project Structure

```
code-plagiarism-detector/
├── src/
│   └── main/
│       ├── java/
│       │   └── com/plagiarism/detector/
│       │       ├── CodePlagiarismDetectorApplication.java   ← Entry point
│       │       ├── algorithm/
│       │       │   ├── RabinKarpMatcher.java    ← Rolling hash segment finder
│       │       │   └── Winnowing.java           ← Fingerprint similarity scorer
│       │       ├── controller/
│       │       │   └── PlagiarismController.java ← REST API endpoints
│       │       ├── model/
│       │       │   └── SimilarityReport.java    ← Response data structure
│       │       ├── service/
│       │       │   └── PlagiarismDetectionService.java ← Orchestration logic
│       │       └── util/
│       │           └── CodeTokenizer.java       ← Lexer and normalizer
│       └── resources/
│           └── application.properties           ← Server configuration
├── pom.xml                                      ← Maven dependencies
└── mvnw / mvnw.cmd                              ← Maven wrapper scripts
```

---

## Environment Setup

### Prerequisites

You need the following installed on your machine:

**1. Java 17 or higher**

Check if Java is installed:
```bash
java -version
```
Expected output: `java version "17.x.x"` or higher.

If not installed, download from: https://adoptium.net (choose Java 17 LTS)

**2. IntelliJ IDEA (recommended) or any Java IDE**

Download Community Edition (free) from: https://www.jetbrains.com/idea/download

**3. Internet connection** (first run only, to download Maven dependencies)

### No additional environment variables or database setup is required.

---

## Running the Application

### Option 1 — Using IntelliJ IDEA (Recommended)

1. Open IntelliJ IDEA
2. Click **File → Open** and select the project folder
3. Click **Trust Project** when prompted
4. Wait for Maven to download dependencies (progress bar at bottom)
5. Open `CodePlagiarismDetectorApplication.java`
6. Click the green **▶ play button** next to the `main` method
7. Wait for the console to show:
   ```
   Started CodePlagiarismDetectorApplication in X seconds
   Tomcat started on port 8080
   ```

### Option 2 — Using Terminal / Command Prompt

Navigate to the project folder and run:

**Windows:**
```cmd
.\mvnw.cmd spring-boot:run
```

**Mac / Linux:**
```bash
./mvnw spring-boot:run
```

### Verifying the Server is Running

Open your browser and go to:
```
http://localhost:8080/api/v1/health
```

You should see:
```
Plagiarism Detector is running!
```

---

## API Reference

The full interactive API documentation is available at:
```
http://localhost:8080/swagger-ui.html
```

### Endpoints

#### 1. Compare Two Files (File Upload)

```
POST /api/v1/detect
Content-Type: multipart/form-data
```

**Parameters:**

| Field | Type | Description |
|---|---|---|
| fileA | File | First source code file (.java, .py, .c, etc.) |
| fileB | File | Second source code file to compare against |

**Example using curl:**
```bash
curl -X POST http://localhost:8080/api/v1/detect \
  -F "fileA=@Student1.java" \
  -F "fileB=@Student2.java"
```

**Limits:** Each file must be under 512 KB.

---

#### 2. Compare Two Code Snippets (JSON)

```
POST /api/v1/detect/text
Content-Type: application/json
```

**Request body:**
```json
{
  "codeA": "public class A { public int add(int a, int b) { return a + b; } }",
  "codeB": "public class B { public int sum(int x, int y) { return x + y; } }",
  "nameA": "Student1.java",
  "nameB": "Student2.java"
}
```

---

#### 3. Health Check

```
GET /api/v1/health
```

Returns `200 OK` with message `"Plagiarism Detector is running!"` if the server is up.

---

## Understanding the Output

A successful comparison returns a JSON report like this:

```json
{
  "fileA": "Student1.java",
  "fileB": "Student2.java",
  "tokenCountA": 22,
  "tokenCountB": 22,
  "jaccardScore": 0.9412,
  "weightedScore": 0.9706,
  "coverageScore": 1.0,
  "finalSimilarityScore": 0.9686,
  "verdict": "HIGH — likely plagiarised",
  "matchedSegments": [
    {
      "startIndexA": 0,
      "endIndexA": 21,
      "startIndexB": 0,
      "endIndexB": 21,
      "length": 22,
      "previewA": "public int VAR_0 ( int VAR_1 , int VAR_2 ) { return VAR_1 + VAR_2 ; }",
      "previewB": "public int VAR_0 ( int VAR_1 , int VAR_2 ) { return VAR_1 + VAR_2 ; }"
    }
  ],
  "analysedAt": "2026-05-16T05:34:48Z"
}
```

### Field Explanations

| Field | Meaning |
|---|---|
| `tokenCountA / B` | Number of meaningful tokens found in each file |
| `jaccardScore` | Fingerprint set overlap ratio (0–1). Conservative measure |
| `weightedScore` | Jaccard + containment blend. Better for partial copies |
| `coverageScore` | Fraction of the shorter file covered by matched segments |
| `finalSimilarityScore` | Final blended score used for the verdict (0–1) |
| `verdict` | Human-readable classification |
| `matchedSegments` | List of exactly which token ranges matched, with previews |
| `previewA / B` | The normalized tokens that matched (not raw source) |
| `analysedAt` | UTC timestamp of when the comparison was performed |

### Verdict Bands

| Score Range | Verdict |
|---|---|
| 0.80 – 1.00 | HIGH — likely plagiarised |
| 0.50 – 0.79 | MEDIUM — suspicious similarity |
| 0.25 – 0.49 | LOW — minor overlap |
| 0.00 – 0.24 | NONE — files appear independent |

---

## Algorithm Deep Dive

### Rabin-Karp Rolling Hash

A rolling hash is a hash function that can be updated in O(1) time as a sliding window moves across a sequence. Instead of recomputing the hash of each k-token window from scratch (which would be O(k) per window), the hash of the new window is derived from the previous one by removing the contribution of the outgoing token and adding the incoming token.

The hash function used is a polynomial hash:

```
hash = (t₀ × BASE^(k-1) + t₁ × BASE^(k-2) + ... + t_(k-1)) mod MOD
```

Where `BASE = 31` and `MOD = 1,000,000,007` (a large prime to minimize collisions).

**Why this catches plagiarism:** Even if a student rearranges the order of methods, any individual method that was copied will produce matching k-gram hashes.

### Winnowing

Winnowing solves the problem of scale: if you have 100 student submissions and want to compare all pairs, you have 4,950 comparisons. Comparing full k-gram sets would be slow.

Winnowing reduces each document to a much smaller "fingerprint" — a representative subset of its k-gram hashes — by selecting the minimum hash in each sliding window. This guarantees that any shared substring longer than a certain threshold will always share at least one fingerprint hash (the "guarantee" property of winnowing).

The Jaccard similarity of two fingerprint sets is then:

```
J(A, B) = |A ∩ B| / |A ∪ B|
```

This is efficient because set intersection and union on sorted sets run in O(n) time.

### Why Three Scores?

- **Jaccard alone** is fair but underestimates similarity when one file is much larger. A student who copies one method from a 500-line file would score very low on Jaccard.
- **Containment** (`|A ∩ B| / min(|A|, |B|)`) normalizes against the smaller file and correctly flags partial copies.
- **Coverage** measures how much of the shorter file's token stream is accounted for by direct Rabin-Karp segment matches, giving a third independent signal.

Blending all three (35% / 35% / 30%) produces a score that is robust against both whole-file copies and surgical partial copies.

---

## Future Scope

### Short Term (Next 3–6 months)

**Multi-language support**
Currently the tokenizer is optimized for Java. Adding language-specific tokenizers for Python, C++, JavaScript, and C# would make the system language-agnostic. Each language needs its own keyword list and token patterns.

**Batch comparison**
Allow uploading a folder of files (e.g., all 30 student submissions at once) and get back a full similarity matrix showing every pair's score. This is the most practically useful feature for academic institutions.

**Database persistence**
Store comparison results in PostgreSQL so that historical comparisons can be queried, reports can be re-downloaded, and trends can be tracked over time.

**AST-based structural analysis**
Currently the system tokenizes at the lexical level. A deeper analysis would parse the code into an Abstract Syntax Tree (AST) and compare tree structures. This catches cases where students reorder if/else branches or change loop types (for → while) to evade detection.

### Medium Term (6–12 months)

**Web frontend**
Build a simple React or Angular UI so non-technical users (teachers, HR reviewers) can upload files through a browser form and see visual side-by-side diffs of matched segments without using Swagger.

**Cross-language detection**
Detect when code has been translated from one language to another (e.g., Python copied and rewritten in Java). This requires language-independent structural fingerprinting.

**Report export**
Generate downloadable PDF or HTML reports with color-highlighted matched segments that can be submitted as evidence.

**User authentication**
Add Spring Security with JWT tokens so that institutions can have their own accounts, submission history, and access controls.

### Long Term (1+ year)

**Machine learning integration**
Train a classifier on known plagiarism cases to better tune the verdict thresholds per language and domain. Neural code embeddings (like CodeBERT) could replace token-based hashing for more semantic similarity detection.

**Scalability**
Move to a distributed architecture where large batches of comparisons are processed in parallel using a message queue (Kafka or RabbitMQ) and worker services, enabling comparison of thousands of files at once.

**IDE plugin**
Build an IntelliJ or VS Code plugin that checks code for similarity against a known corpus as the developer types — useful for companies that want to ensure no proprietary code is being copied.

---

## Configuration Reference

All settings are in `src/main/resources/application.properties`:

```properties
# Port the server listens on
server.port=8080

# Maximum size of a single uploaded file
spring.servlet.multipart.max-file-size=2MB

# Maximum total size of a multipart request
spring.servlet.multipart.max-request-size=5MB

# Log level for the application code
logging.level.com.plagiarism=DEBUG
```

To change the port (e.g., to 9090), update `server.port=9090` and restart.

---

## Tuning the Detection Sensitivity

In `PlagiarismDetectionService.java`, three constants control sensitivity:

```java
private static final int KGRAM_SIZE  = 7;   // Token window size for hashing
private static final int WINDOW_SIZE = 4;   // Winnowing window size
private static final int MIN_MATCH   = 5;   // Minimum segment length to report
```

| Constant | Effect of increasing | Effect of decreasing |
|---|---|---|
| `KGRAM_SIZE` | Fewer but more specific matches | More matches, more false positives |
| `WINDOW_SIZE` | Smaller fingerprint, faster but less precise | Larger fingerprint, slower but more precise |
| `MIN_MATCH` | Ignores shorter copied segments | Reports even trivial single-line matches |

For short code snippets (under 50 lines), consider reducing `KGRAM_SIZE` to 4 or 5.

---

## Author

Built as a professional fresher project demonstrating applied Data Structures & Algorithms in a production Spring Boot backend.

**Technologies demonstrated:** Lexical analysis, polynomial rolling hashes, sliding window algorithms, set-based similarity metrics, RESTful API design, file processing, and OpenAPI documentation.
