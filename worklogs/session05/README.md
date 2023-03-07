# Session 05 worklog 
## Introduce a DB abstraction layer in your ITU-MiniTwit

### Object-relational mapping (ORM)
Although the implementation of this feature has not yet been realized, we plan to employ Spring Data JPA for the purpose of mapping. Presently, the system lacks the Object-Relational Mapping (ORM) framework, but we intend to integrate it in future iterations. Although the ORM framework is absent the system is still fully functional, but do not have the persistence we want. Here are some of the reasons why we opted for this technology.

Easy to use: Spring Data JPA provides a simple and easy-to-use interface for accessing the database. It uses standard JPA annotations to map objects to tables, which makes it easy to use.

Supports multiple databases: Spring Data JPA supports multiple databases, including MySQL, PostgreSQL, Oracle, and others. This means that we can easily switch to a different database in the future.

Integration with Spring Boot: Spring Data JPA integrates seamlessly with Spring Boot (as we a currently using), which makes it easy to configure and use in our application.

Rich feature set: Spring Data JPA provides a rich feature set, including support for caching, pagination, and auditing. This can help improve the performance and scalability.

### DBMS
We have made a decision to utilize a managed MySQL database solution hosted on Digital Ocean as our preferred database management system (DBMS). Below are some key considerations that influenced our choice.

1. Open source: MySQL is an open-source DBMS.

2. Scalability: MySQL is scalable and can handle large amounts of data. It also supports sharding, which allows us to distribute data across multiple servers, that will be needed for future load on our system.

3. Performance: MySQL is known for its high performance and can handle a large number of transactions per second, which is needed in our twitter application.

4. Community support: MySQL has a large and active community of users and developers, which means that we can find plenty of resources and help online.

We have also consindered upon the possibility of using a NoSQL graph database platform, specifically Neo4j, due to its sophisticated search capabilities and algorithmic recommendation engine that facilitates e.g. which user would like another user content. However, given the current time constraints, we have determined that the process of transitioning to this alternative database management system would be to time-consuming. This would require a mapping of the current database to a CSV file and subsequently design a transformer capable of translating it into a suitable Graph data structure.
