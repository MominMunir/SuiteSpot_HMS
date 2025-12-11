# SuiteSpot - Hotel Management System

A comprehensive hotel management system built with Spring Boot, designed to streamline hotel operations including room management, guest bookings, check-in/check-out processes, billing, and administrative tasks.

## 🚀 Features

- **Room Management**: Manage hotel rooms with different types (Single, Double, Suite, Deluxe, Presidential), track availability, and set pricing
- **Guest Management**: Maintain guest profiles with contact information, identification details, and preferences
- **Booking System**: Create and manage reservations with check-in/check-out date tracking
- **Check-In/Check-Out**: Streamlined processes for guest arrivals and departures
- **Billing & Payments**: Generate bills, track payment status, and manage room charges, service charges, and taxes
- **Taxi Service**: Request and manage taxi services for guests
- **Admin Dashboard**: Comprehensive dashboard with room occupancy statistics and recent bookings
- **User Authentication**: Secure login system with role-based access control (Admin, Staff, Guest)
- **Database Backup**: Automated database backup functionality using PostgreSQL's pg_dump
- **System Settings**: Configurable hotel settings including tax rates, service charges, and policies

## 🛠️ Tech Stack

- **Framework**: Spring Boot 3.2.0
- **Language**: Java 17
- **Database**: PostgreSQL
- **ORM**: Spring Data JPA / Hibernate
- **Security**: Spring Security
- **Templating**: Thymeleaf
- **Build Tool**: Maven
- **Other**: Lombok, Spring Boot Actuator

## 📋 Prerequisites

Before you begin, ensure you have the following installed:

- **Java Development Kit (JDK)**: Version 17 or higher
- **Maven**: Version 3.6+ 
- **PostgreSQL**: Version 12 or higher
- **Git**: For version control

## 🔧 Installation & Setup

### 1. Clone the Repository

```bash
git clone https://github.com/yourusername/suite_spot.git
cd suite_spot
```

### 2. Database Setup

1. Create a PostgreSQL database:
```sql
CREATE DATABASE suitespot;
```

2. Update database credentials in `src/main/resources/application.properties`:
```properties
spring.datasource.url=jdbc:postgresql://localhost:6969/suitespot
spring.datasource.username=your_username
spring.datasource.password=your_password
```

3. (Optional) Run the database setup script to create tables and insert sample data:
```bash
psql -U postgres -d suitespot -f scripts/database-setup.sql
```

**Note**: The script uses MySQL syntax. For PostgreSQL, you may need to adjust the syntax or let Spring Boot create the tables automatically using JPA.

### 3. Configure Backup Path (Optional)

If you want to use the database backup feature, update the pg_dump path in `application.properties`:

```properties
backup.pg-dump.path=C:\\Program Files\\PostgreSQL\\18\\bin\\pg_dump.exe
```

For Linux/Mac:
```properties
backup.pg-dump.path=/usr/bin/pg_dump
```

### 4. Build the Project

```bash
mvn clean install
```

### 5. Run the Application

```bash
mvn spring-boot:run
```

Or run the JAR file:
```bash
java -jar target/hotel-management-1.0.0.jar
```

The application will be available at: `http://localhost:5173`

## 🔐 Default Credentials

After running the database setup script, you can log in with:

- **Username**: `admin`
- **Password**: `admin123`

**⚠️ Important**: Change the default password after first login in a production environment!

## 📁 Project Structure

```
suite_spot/
├── src/
│   ├── main/
│   │   ├── java/com/suitespot/
│   │   │   ├── config/          # Configuration classes
│   │   │   ├── controller/      # REST/Web controllers
│   │   │   ├── entity/          # JPA entities
│   │   │   ├── repository/      # Data access layer
│   │   │   ├── service/         # Business logic layer
│   │   │   └── HotelManagementApplication.java
│   │   └── resources/
│   │       ├── application.properties
│   │       └── templates/       # Thymeleaf templates
│   └── test/                    # Test files
├── scripts/
│   └── database-setup.sql       # Database initialization script
├── pom.xml                      # Maven dependencies
└── README.md                    # This file
```

## 🎯 Usage

### Accessing the Application

1. Navigate to `http://localhost:5173` in your web browser
2. Log in with your credentials
3. Use the navigation menu to access different features:
   - **Dashboard**: View room statistics and recent bookings
   - **Rooms**: Manage hotel rooms
   - **Guests**: Manage guest profiles
   - **Bookings**: Create and manage reservations
   - **Check-In/Check-Out**: Process guest arrivals and departures
   - **Taxi**: Manage taxi requests
   - **Admin**: Access administrative functions (Admin only)

### Key Workflows

1. **Creating a Booking**:
   - Add a guest profile (if not exists)
   - Search for available rooms
   - Create a booking with check-in/check-out dates
   - Process check-in when guest arrives

2. **Check-Out Process**:
   - Search for active bookings
   - Generate bill
   - Process payment
   - Complete check-out

## 🧪 Testing

Run tests using Maven:

```bash
mvn test
```

## 📝 Configuration

Key configuration options in `application.properties`:

- **Server Port**: `server.port=5173`
- **Database Connection**: Configure PostgreSQL connection details
- **JPA Settings**: Hibernate DDL mode (`update` for development, `validate` for production)
- **Logging**: Adjust log levels as needed

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## 📄 License

This project is licensed under the MIT License - see the LICENSE file for details.

## 👥 Authors

- **Your Name** - *Initial work*

## 🙏 Acknowledgments

- Spring Boot team for the excellent framework
- PostgreSQL community
- All contributors and users of this project

## 📞 Support

For support, email support@suitespot.com or open an issue in the GitHub repository.

---

**Note**: This is a development version. For production deployment, ensure proper security configurations, use environment variables for sensitive data, and follow Spring Boot production best practices.

