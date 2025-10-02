# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview
AI 융합 모의면접 & 취업컨설팅 웹 애플리케이션 (AI Convergence Mock Interview & Employment Consulting Web Application)

## Architecture
This is a full-stack application with:
- **Backend**: Spring Boot 3.5.4 with Java 17 (multi-module Gradle project)
- **Frontend**: React with Vite, using JSX components and CSS modules
- **Database**: MySQL with JPA/Hibernate
- **Cache**: Redis (via docker-compose)
- **Authentication**: OAuth2 (Google, Kakao, Naver) + JWT + Email verification
- **Video Calls**: OpenVidu integration for interview sessions
- **Payments**: Integrated payment system for consultation bookings
- **Email**: SMTP email service for verification and notifications

## Common Commands

### Backend (Spring Boot)
```bash
# Build and run tests
./gradlew build

# Run tests only
./gradlew test

# Run the application
./gradlew bootRun

# Clean build
./gradlew clean build
```

### Frontend (React + Vite)
```bash
cd frontend

# Development server
npm run dev

# Production build
npm run build

# Lint code
npm run lint

# Preview production build
npm run preview
```

### Docker Services
```bash
# Start Redis (required for backend)
docker-compose up -d

# Stop services
docker-compose down
```

## Key Application Modules

### Authentication & Authorization
- OAuth2 integration with Google, Kakao, and Naver
- JWT token management with refresh tokens
- Email verification system with signed tokens
- Role-based access control (USER, CONSULTANT, ADMIN)
- Blacklist management for token invalidation

### Core Business Domains
- **User Management**: User profiles, consultant metadata, role management
- **Booking System**: Appointment scheduling, availability management, consultation booking
- **Payment System**: Order processing, payment integration, refund handling
- **Review System**: Post-consultation reviews and ratings
- **Video Conferencing**: OpenVidu integration for real-time mock interviews

### Security Configuration
- JWT authentication filter
- CORS configuration for frontend integration
- OAuth2 success/failure handlers
- Signed cookie utilities for secure data transmission

## File Structure Patterns

### Backend (`backend/src/main/java/com/codelab/micproject/`)
- Domain-driven structure with packages: `domain/`, `service/`, `repository/`, `controller/`, `dto/`
- Security components in `security/` with JWT and OAuth2 implementations
- Common utilities in `common/` (exceptions, responses, utilities)
- Configuration classes in `config/`

### Frontend (`frontend/`)
- Pages in `pages/` directory (React components with corresponding CSS files)
- Shared components and styles in `components/`
- API configuration in `api/axios.js`
- Assets (SVG icons) in `assets/`

## Development Notes
- The project uses multi-module Gradle with root-level settings
- Frontend uses Vite for fast development and building
- Email verification flows use both pre-signup tokens and post-signup verification
- OAuth2 flow includes temporary principal handling for social login
- Payment system supports order batching for multiple appointment bookings
- Redis is used for caching and session management