# Nike-X E-Commerce Platform

Nike-X is a full-stack, Java-based e-commerce platform built as an academic portfolio project. It provides a comprehensive set of features for both customers and administrators, handling everything from product browsing and shopping carts to inventory management and secure checkouts.

## Key Features

- **Product Catalog & Advanced Search**: Browse products by category, brand, size, color, or use dynamic text search.
- **Shopping Cart System**: Session-based cart for guests; persistent database cart for logged-in users.
- **User Authentication**: Secure login, registration, and email verification.
- **Admin Dashboard**: Comprehensive management of Products, Users, Suppliers, and Goods Receipt Notes (GRN).
- **Security Protocols**: Built-in rate limiting, CSRF protection, and OWASP-recommended security headers.

## Tech Stack

- **Backend Language**: Java 17
- **Architecture**: RESTful web services (JAX-RS) and Servlet filters
- **Database & ORM**: MySQL with Hibernate
- **Build Tool**: Maven

## Repository Structure

- `src/main/java/`: Contains all backend Java source files (Services, Models, DTOs, Controllers/Middlewares).
- `src/main/webapp/`: Contains all frontend assets (HTML, CSS, JS, Images).

## Setup Instructions

1. **Clone the repository:**
   ```bash
   git clone https://github.com/SenanThewnaka/nike-x.git
   ```

2. **Configure the Database:**
   - Ensure MySQL is running on your machine.
   - Update your connection variables (username, password, URL) in `.env` or `hibernate.cfg.xml`.

3. **Build the Application:**
   Run the following Maven command to build the project.
   ```bash
   mvn clean package
   ```

4. **Deploy:**
   Deploy the resulting `.jar` or `.war` file to your server of choice (e.g. Apache Tomcat, Jetty, or via cloud deployment on Render/Koyeb).

## License
Provided under a Custom Non-Commercial License. You may copy, modify, and run this project strictly for educational and academic purposes, but you may **not** use, sell, or sublicense it for commercial purposes.
