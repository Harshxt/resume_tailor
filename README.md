# ResumeTailor 🎯

An AI-powered resume tailoring assistant that analyzes resumes against specific job descriptions to provide targeted suggestions, missing keywords, and actionable improvements.

[![Java 21](https://img.shields.io/badge/Java_21-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot_4-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![Spring AI](https://img.shields.io/badge/Spring_AI-6DB33F?style=for-the-badge&logo=spring&logoColor=white)](https://spring.io/projects/spring-ai)
[![Google Gemini](https://img.shields.io/badge/Google_Gemini-8E75C4?style=for-the-badge&logo=googlegemini&logoColor=white)](https://ai.google.dev/)
[![Apache Tika](https://img.shields.io/badge/Apache_Tika-D22128?style=for-the-badge&logo=apache&logoColor=white)](https://tika.apache.org/)
<!-- [![React 19](https://img.shields.io/badge/React_19-20232A?style=for-the-badge&logo=react&logoColor=61DAFB)](https://react.dev/)
[![TypeScript](https://img.shields.io/badge/TypeScript-3178C6?style=for-the-badge&logo=typescript&logoColor=white)](https://www.typescriptlang.org/)
[![Vite](https://img.shields.io/badge/Vite-646CFF?style=for-the-badge&logo=vite&logoColor=white)](https://vitejs.dev/) -->


---

## 📑 Table of Contents

- [Overview](#overview)
- [Key Features](#key-features)
- [Tech Stack](#tech-stack)
- [Architecture & Flow](#architecture--flow)
- [Project Structure](#project-structure)
- [Prerequisites](#prerequisites)
- [Getting Started](#getting-started)
  - [1. Backend Setup](#1-backend-setup)
- [Configuration & Environment Variables](#configuration--environment-variables)
- [API Reference](#api-reference)
- [Roadmap](#roadmap)
<!-- - [2. Frontend Setup](#2-frontend-setup) -->

---

## 🌟 Overview

**ResumeTailor** bridges the gap between job candidates and applicant tracking systems (ATS) / hiring managers. Candidates can upload their resume in PDF or Word format alongside a target job description and role. ResumeTailor extracts and structures the resume contents, debloats noisy job descriptions, verifies document validity, and leverages Gemini Generative AI to provide targeted, actionable recommendations.

---

## ✨ Key Features

- **Document Ingestion & Extraction**: Automatically parses PDF and DOCX documents using Apache Tika.
- **Smart Validation**: AI-driven check to ensure uploaded documents are actual resumes before running heavy analysis.
- **Job Description Debloating**: Strips boilerplate text and irrelevancies from lengthy job descriptions to focus on key requirements.
- **Tailored Improvement Suggestions**: Delivers granular, role-specific feedback covering missing skills, phrasing improvements, and structural adjustments formatted in clean Markdown.
<!-- - **Modern Responsive UI**: Built with React 19, TypeScript, and Vite with dark/light mode toggle and interactive preview. -->

---

## 🛠️ Tech Stack

### Backend
- **Language**: Java 21
- **Framework**: Spring Boot 4.1.x
- **AI Integration**: Spring AI (2.0.x) with Google GenAI (`gemini-3.1-flash-lite`)
- **Document Parsing**: Apache Tika (`TikaDocumentReader`)
- **Database**: H2 in-memory database (with `/h2-console` enabled for debugging)
- **Build Tool**: Maven (`mvnw`)

<!-- ### Frontend
- **Framework**: React 19 + TypeScript
- **Bundler**: Vite
- **Markdown Rendering**: Marked
- **Styling**: Modern CSS with CSS custom properties (variables) supporting light & dark themes -->

---

## 🔄 Architecture & Flow

```
[User Browser]
       │
       ▼ (Multipart Form: File + JSON Metadata)
[Spring Boot REST Controller] (`/api/resume/upload`)
       │
       ├──> [StorageService] -> Saves file locally (`./files`)
       │
       ├──> [ResumeParserService] -> Extracts text via Apache Tika into structured DTO
       │
       └──> [LlmService] (Spring AI + Google Gemini)
               ├── 1. Validates document is a resume
               ├── 2. Debloats & cleans job description
               └── 3. Generates targeted resume enhancement advice
       │
       ▼ (JSON Response with suggestions in Markdown)

```

---

## 📁 Project Structure

```text
resumeTailor/
├── .env.local                          # Local environment variables (API keys)
├── pom.xml                             # Maven configuration & Spring dependencies
├── files/                              # Directory for stored uploaded resumes
└── src/
    ├── main/
    │   ├── java/one/harshit/resumeTailor/
    │   │   ├── controller/             # REST Controller and API request/response records
    │   │   ├── exception/advice/       # Global exception handling
    │   │   ├── model/                  # Data entities, DTOs, and type converters
    │   │   ├── repository/             # Data access interfaces
    │   │   ├── service/                # Parser, Storage, and Spring AI LLM services
    │       ├── application.yaml        # Spring Boot application configuration
    │   │   └── ResumeTailorApplication.java # Spring Boot main entrypoint
    │   └── resources/
    │       └── static/ & templates/
    └── test/                           # Unit & integration tests

```

---

## 📋 Prerequisites

Make sure you have the following installed on your machine:
- **Java**: JDK 21 or higher
- **Node.js**: v18+ (Node 20+ recommended)
- **npm** or **yarn** / **pnpm**
- **Google Gemini API Key**: Obtainable via [Google AI Studio](https://aistudio.google.com/)

---

## 🚀 Getting Started

### 1. Backend Setup

1. **Clone the repository**:
   ```bash
   git clone https://github.com/Harshxt/resume_tailor.git
   cd resumeTailor
   ```

2. **Configure environment variables**:
   Create a `.env.local` file in the project root:
   ```properties
   GEMINI_API_KEY=your_gemini_api_key_here
   ```

3. **Run the backend**:
   ```bash
   ./mvnw spring-boot:run
   ```
   The backend server will start on `http://localhost:8080`.
   - Health check: `http://localhost:8080/api/resume/health`
   - H2 Database Console: `http://localhost:8080/h2-console` (JDBC URL: `jdbc:h2:mem:tailor_db`)

---

<!-- ### 2. Frontend Setup

1. **Navigate to the frontend directory**:
   ```bash
   cd frontend/resumeTailor_frontend
   ```

2. **Install dependencies**:
   ```bash
   npm install
   ```

3. **Start the development server**:
   ```bash
   npm run dev
   ```
   The frontend will be available at `http://localhost:5173`. -->

---

## ⚙️ Configuration & Environment Variables

| Variable | Description | Default | Location |
| :--- | :--- | :--- | :--- |
| `GEMINI_API_KEY` | Google Gemini API key for Spring AI | *Required* | `.env.local` |
| `LOGGING_LEVEL_ONE_HARSHIT_RESUMETAILOR` | Application logging level | `info` | `.env.local` / Environment |

---

## 📡 API Reference

### Health Check
- **Endpoint**: `GET /api/resume/health`
- **Response**: `200 OK` (`"All good"`)

### Upload & Tailor Resume
- **Endpoint**: `POST /api/resume/upload`
- **Content-Type**: `multipart/form-data`
- **Request Parts**:
  - `file` (*MultipartFile*, required): Resume file (`.pdf` or `.docx`).
  - `data` (*JSON String*, optional):
    ```json
    {
      "jobDescription": "Full Job Description text...",
      "targetRole": "Software Engineer"
    }
    ```
- **Sample Success Response**:
  ```json
  {
    "success": true,
    "message": "Received file: resume.pdf",
    "data": {
      "suggestions": "### Key Recommendations\n- Add experience with Docker..."
    }
  }
  ```

---

## 🗺️ Roadmap

- [ ] Frontend with form, proper markdown parsing and document editing
- [ ] ATS compatibility score metric (% match against JD)
- [ ] Authentication and rate-limiting
- [ ] Queries history: Store queries history for user reference.
