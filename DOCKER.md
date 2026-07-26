# ReferralPro - Docker Setup

This project uses Docker Compose to orchestrate three services:
- **MySQL Database** (port 3307)
- **Spring Boot Backend** (port 8080)
- **Angular Frontend** (port 80)

## Prerequisites

- Docker Desktop installed and running
- Ports 80, 8080, and 3307 available

## Quick Start

### Build and run all services:
```powershell
docker-compose up -d --build
```

### View logs:
```powershell
# All services
docker-compose logs -f

# Specific service
docker-compose logs -f backend
docker-compose logs -f frontend
docker-compose logs -f mysql
```

### Stop all services:
```powershell
docker-compose down
```

### Stop and remove volumes (clean slate):
```powershell
docker-compose down -v
```

## Service URLs

- **Frontend (Angular)**: http://localhost
- **Backend API**: http://localhost:8080/api
- **MySQL**: localhost:3307

## Building Individual Services

### Backend only:
```powershell
docker build -t referralpro-backend .
```

### Frontend only:
```powershell
docker build -t referralpro-frontend ./referralPro-dashboard
```

## Architecture

### Backend (Spring Boot)
- Multi-stage build using Maven
- Based on Eclipse Temurin 21
- Connects to MySQL container via Docker network
- Environment variables configured in docker-compose.yml

### Frontend (Angular)
- Multi-stage build (Node.js → nginx)
- Production build served by nginx
- Configured with CORS support
- Health check endpoint at /health

### Database (MySQL)
- MySQL 8.4
- Persistent volume for data
- Health checks enabled
- Initialized with Flyway migrations

## Development vs Production

For **local development**, use:
```powershell
# Backend
.\mvnw.cmd spring-boot:run

# Frontend
cd referralPro-dashboard
npm start
```

For **Docker deployment** (testing production build locally):
```powershell
docker-compose up --build
```

For **production deployment**:
1. Update `environment.prod.ts` with your production API URL
2. Update `application.yml` with production database credentials
3. Set secure JWT secret in environment variables
4. Use proper SSL/TLS certificates
5. Configure reverse proxy (nginx/traefik) if needed

## Troubleshooting

### Backend can't connect to database:
```powershell
# Check if MySQL is healthy
docker-compose ps

# Wait for MySQL to be ready
docker-compose logs mysql
```

### Frontend shows CORS errors:
- Ensure backend is running and accessible
- Check backend CORS configuration in SecurityConfig.java

### Port conflicts:
```powershell
# Change ports in docker-compose.yml:
# ports:
#   - "8081:8080"  # Backend on 8081 instead of 8080
#   - "8080:80"    # Frontend on 8080 instead of 80
```

## Notes

- The backend waits for MySQL to be healthy before starting
- Frontend waits for backend to be ready
- All services are connected via a Docker bridge network
- Data persists in the `mysql_data` volume
