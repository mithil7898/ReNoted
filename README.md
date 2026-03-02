# ReNoted - A Notion-like Note-Taking Application

## 🎯 Project Overview

ReNoted is a full-stack web application inspired by Notion, built for learning modern web development with Spring Boot and React.

**Current Version:** v0.1 - Project Foundation

## 🛠️ Tech Stack

### Backend
- **Java 23**
- **Spring Boot 3.4.1**
- **Spring Data JPA** (Hibernate)
- **PostgreSQL 17.2**
- **Maven** (build tool)

### Frontend
- **React 18**
- **Vite** (build tool)
- **Tailwind CSS** (styling)
- **Axios** (HTTP client)
- **Node.js 22.13.0**

## 📦 Project Structure
```
ReNoted/
├── backend/                    # Spring Boot REST API
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/renoted/
│   │   │   │   ├── config/           # CORS configuration
│   │   │   │   ├── controller/       # REST endpoints
│   │   │   │   ├── service/          # Business logic (future)
│   │   │   │   ├── repository/       # Database access (future)
│   │   │   │   ├── entity/           # JPA entities (future)
│   │   │   │   └── dto/              # Data transfer objects (future)
│   │   │   └── resources/
│   │   │       └── application.properties
│   │   └── test/
│   └── pom.xml
└── frontend/                   # React application
    ├── src/
    │   ├── components/         # Reusable components (future)
    │   ├── pages/              # Page components
    │   │   └── Home.jsx        # Landing page
    │   ├── services/           # API calls
    │   │   └── api.js          # Axios configuration
    │   └── utils/              # Helper functions (future)
    ├── package.json
    └── tailwind.config.js
```

## 🚀 Getting Started

### Prerequisites

- **Java 17+** (we're using Java 23)
- **Node.js 20.19+ or 22.12+** (we're using 22.13.0)
- **PostgreSQL 14+** (we're using 17.2)
- **Maven 3.8+**
- **Git**

### Backend Setup

1. **Navigate to backend folder:**
```bash
   cd backend
```

2. **Create PostgreSQL database:**
```bash
   psql -U postgres
```
```sql
   CREATE DATABASE renoted;
   \q
```

3. **Configure database credentials:**
   - Edit `src/main/resources/application.properties`
   - Update `spring.datasource.password` with your PostgreSQL password

4. **Run the application:**
```bash
   ./mvnw spring-boot:run
```
   Or run `ReNotedApplication.java` in IntelliJ IDEA

5. **Backend runs on:** http://localhost:8080

6. **Test endpoints:**
   - Health check: http://localhost:8080/api/health
   - Welcome: http://localhost:8080/api/welcome

### Frontend Setup

1. **Navigate to frontend folder:**
```bash
   cd frontend
```

2. **Install dependencies:**
```bash
   npm install
```

3. **Run development server:**
```bash
   npm run dev
```

4. **Frontend runs on:** http://localhost:5173

## 🎯 Current Features (v0.1)

- ✅ Spring Boot backend with REST API
- ✅ PostgreSQL database connection configured
- ✅ React frontend with Vite and Tailwind CSS
- ✅ Frontend-backend integration via Axios
- ✅ CORS configuration for cross-origin requests
- ✅ Health check endpoints
- ✅ Layered architecture (Controller → Service → Repository)
- ✅ Beautiful responsive UI
- ✅ Error handling and loading states

## 🗺️ Development Roadmap

- **v0.1** ✅ - Project foundation and environment setup
- **v0.2** 🚧 - Core CRUD operations for notes
- **v0.3** 📋 - Tags system and search functionality
- **v0.4** 📝 - Markdown support and rich text editor
- **v1.0** 🔐 - User authentication with JWT
- **v1.1** 📚 - Version history for notes
- **v2.0** 🔗 - Backlinks and graph visualization

## 📚 Learning Outcomes

This project demonstrates understanding of:

- RESTful API design
- Layered/Clean architecture
- React component architecture
- State management with hooks
- HTTP communication (Axios)
- CORS and security basics
- Responsive design with Tailwind CSS
- Modern development tooling (Vite, Maven)
- Database configuration and connection
- Error handling and user feedback

## 👨‍💻 Author

Built as a learning project to master Spring Boot and React development.

## 📝 License

MIT License - Free to use for learning purposes!

---

**Built with ❤️ and lots of coffee ☕**