PayPulse

PayPulse is a Java-based payment management application developed using Java and MySQL.

Technologies Used

* Java
* MySQL
* JDBC
* IntelliJ IDEA

Project Structure

PayPulse/
├── src/
│   ├── main/
│   │   └── java/
│   └── test/
│       └── java/
├── lib/
│   └── mysql-connector-j-26.10.0.jar
├── schema.sql
├── db.properties.example
├── README.md
├── .gitignore
└── PayPulse.iml

Setup Instructions

1. Clone the repository.
2. Open the project in IntelliJ IDEA.
3. Configure the required JDK.
4. Add the MySQL Connector JAR as a project library.
5. Create a MySQL database.
6. Execute the SQL commands in schema.sql.
7. Create a local db.properties file using db.properties.example.
8. Update your local database credentials.

Database Configuration

Database credentials are stored locally in db.properties.

Never commit your actual database credentials to GitHub.