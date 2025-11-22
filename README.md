# Unified Notification Microservice

A production-ready, reactive microservice built with Spring Boot 3.x and Java 21 that consumes events from Kafka and sends notifications via WhatsApp, Email, and Real-Time channels.

## Features

- **Reactive Programming**: Built entirely with Spring WebFlux and Project Reactor
- **Multi-Channel Notifications**: WhatsApp, Email (SMTP), and Real-Time (WebSocket)
- **Event-Driven Architecture**: Kafka consumer with dead letter topic for failed messages
- **Persistent Logging**: PostgreSQL with R2DBC for reactive database operations
- **Retry Mechanism**: Exponential backoff retry with configurable attempts
- **Observability**: Prometheus metrics, health checks, and structured logging
- **Production-Ready**: Docker support, proper error handling, and scalable design

## Tech Stack

- **Backend**: Java 21, Spring Boot 3.4.0, Spring WebFlux
- **Messaging**: Apache Kafka with Reactor Kafka
- **Database**: PostgreSQL with R2DBC (Reactive Driver)
- **Notifications**: 
  - WhatsApp Cloud API (Meta Graph API)
  - SMTP (Spring Boot Mail)
  - WebSocket for Real-Time
- **Monitoring**: Micrometer, Prometheus
- **Build Tool**: Maven
- **Containerization**: Docker & Docker Compose

## Prerequisites

- Java 21 or higher
- Maven 3.8+
- Docker & Docker Compose (for containerized deployment)
- PostgreSQL 16+
- Apache Kafka 3.5+

## Getting Started

### 1. Clone the Repository

\`\`\`bash
git clone <repository-url>
cd unified-notification-service
\`\`\`

### 2. Configure Environment Variables

Copy the example environment file and update with your credentials:

\`\`\`bash
cp .env.example .env
\`\`\`

Edit `.env` and configure:
- Database credentials
- Kafka bootstrap servers
- SMTP settings (Gmail or other provider)
- WhatsApp API credentials (from Meta Developer Portal)
- Firebase credentials (if using Firebase for push notifications)

### 3. Run with Docker Compose

The easiest way to run the entire stack:

\`\`\`bash
docker-compose up -d
\`\`\`

This starts:
- PostgreSQL database
- Zookeeper & Kafka
- Notification Service
- Prometheus (for metrics)

### 4. Run Locally (Development)

Start dependencies:

\`\`\`bash
docker-compose up -d postgres kafka zookeeper
\`\`\`

Run the application:

\`\`\`bash
./mvnw spring-boot:run
\`\`\`

## API Endpoints

### Health Check
\`\`\`
GET http://localhost:8080/api/health
\`\`\`

### Actuator Endpoints
\`\`\`
GET http://localhost:8080/actuator/health
GET http://localhost:8080/actuator/prometheus
GET http://localhost:8080/actuator/metrics
\`\`\`

### WebSocket Connection
\`\`\`
ws://localhost:8080/ws/notifications?userId=USER-12345
\`\`\`

## Kafka Message Format

Send messages to the `notifications` topic with the following JSON structure:

\`\`\`json
{
  "user_name": "Anand Junjharawad",
  "username": "anand_j",
  "subject": "Ride Assigned",
  "source": "WHATSAPP",
  "destination": {
    "whatsapp_number": "+919876543210",
    "email": "anand@example.com",
    "user_id": "USER-12345"
  },
  "message": "Your driver is arriving in 2 minutes.",
  "metadata": {
    "priority": "HIGH",
    "timestamp": "2025-02-01T12:34:56Z",
    "ride_id": "RIDE-7788"
  }
}
\`\`\`

### Supported Notification Sources

- `WHATSAPP`: Sends via WhatsApp Cloud API
- `EMAIL`: Sends via SMTP
- `REALTIME`: Sends via WebSocket to connected clients

## Database Schema

The service automatically creates the required schema on startup. See `src/main/resources/schema.sql` for details.

Main table: `notification_logs`
- Stores all notification attempts
- Tracks status (SUCCESS, FAILED, RETRY)
- Includes retry count and error messages
- Indexed for performance

## Configuration

Key configuration properties in `application.yml`:

- **Database**: Connection pool, R2DBC settings
- **Kafka**: Consumer group, topics, retry settings
- **Notifications**: Retry attempts, backoff delays, timeouts
- **Monitoring**: Actuator endpoints, Prometheus metrics

## Monitoring & Observability

### Prometheus Metrics
Access metrics at: `http://localhost:8080/actuator/prometheus`

### Prometheus UI
Access Prometheus at: `http://localhost:9090`

### Logs
- Console logs with structured format
- File logs in `logs/notification-service.log`
- Rotating log files (10MB max, 30 days retention)

## Error Handling

- **Retry Mechanism**: Exponential backoff with configurable max attempts
- **Dead Letter Topic**: Failed messages sent to `notifications-dlt`
- **Graceful Degradation**: Logs failures and continues processing
- **Global Exception Handler**: Standardized error responses

## WhatsApp Setup

1. Create a Meta Developer account
2. Create a WhatsApp Business App
3. Get Phone Number ID and Access Token
4. Add these to your `.env` file

## Email Setup (Gmail Example)

1. Enable 2-Factor Authentication on Gmail
2. Generate App Password: Google Account → Security → App Passwords
3. Use App Password in `SMTP_PASSWORD`

## Production Deployment

### Build JAR
\`\`\`bash
./mvnw clean package -DskipTests
\`\`\`

### Build Docker Image
\`\`\`bash
docker build -t notification-service:1.0.0 .
\`\`\`

### Environment Variables for Production
- Use secrets management (e.g., Kubernetes Secrets, AWS Secrets Manager)
- Enable TLS/SSL for Kafka and database
- Set `logging.level.root=WARN` for production
- Configure proper resource limits

## Testing

Run unit tests:
\`\`\`bash
./mvnw test
\`\`\`

Run integration tests:
\`\`\`bash
./mvnw verify
\`\`\`

## Scalability Considerations

- **Stateless Design**: Can run multiple instances behind a load balancer
- **Kafka Consumer Groups**: Parallel processing across instances
- **R2DBC Connection Pooling**: Efficient database resource usage
- **Reactive Streams**: Non-blocking I/O for high throughput
- **WebSocket Session Management**: Thread-safe concurrent map

## Security Best Practices

- Never commit `.env` or credentials
- Use environment variables for all secrets
- Run container as non-root user (configured in Dockerfile)
- Enable HTTPS in production
- Implement rate limiting for API endpoints
- Use Kafka ACLs and authentication

## Troubleshooting

### Kafka Connection Issues
- Ensure Kafka is running: `docker ps`
- Check bootstrap servers configuration
- Verify topic exists: `kafka-topics --list --bootstrap-server localhost:9092`

### Database Connection Issues
- Check PostgreSQL is running
- Verify credentials in `.env`
- Check database logs: `docker logs notification-postgres`

### WhatsApp API Errors
- Verify access token is valid
- Check phone number format (E.164: +[country][number])
- Review Meta API error codes

## Contributing

1. Fork the repository
2. Create a feature branch
3. Commit your changes
4. Push to the branch
5. Create a Pull Request

## License

This project is licensed under the MIT License.

## Support

For issues and questions, please create an issue in the repository.
