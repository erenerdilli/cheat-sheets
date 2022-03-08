/*
JPA (Java Persistence API)
Transaction Management with an Entity-Mananger:
---
entityManager.getTransaction().begin();
entityManager.persist(<some-entity>);
entityManager.getTransaction().commit();
entityManager.clear();
SomeEntity entity = entityManager.find(SomeEntity.class, 1);
---
@OneToOne's fetch type is EAGER by default
Lists + Sets fetch type is LAZY by default
Two types of Lazy Loading implementations
1. Proxying the object (default in Hibernate) by creating a Subclass of that object at runtime and overwrite the get methods.
   This is done by the JavaAssist lib.
2. ByteCode Enhancement (default in EclipseLink): Add special logic to the get methods inside the Java Bytecode
LazyInitializationExcepiton: 
Entity Lifecycle
New ---> em.persist ---> Managed
New ---> em.merge ---> Managed
Managed ---> em.remove ---> Removed
Managed ---> em.find ---> Managed
Managed ---> query.getResultList ---> Managed
Managed ---> query.getSingleResult ---> Managed
Managed ---> em.detach ---> Detached
Managed ---> em.close ---> Detached
Managed ---> em.clear ---> Detached
Detached ---> em.merge ---> Managed
Ein neu angelegtes Entity Object ist im Zustand "New".
Managed - Es gibt einen Entity-Manager, der für dieses Objekt verantwortlich ist:
  Vorteile: - Es werden automatisch Änderungen getrackt.
              Beim nächsten Transaktions-Commit werden nur die Änderungen in die DB geschrieben.
              
  Lazy Loading funktioniert
Detached - Lazy Loading muss nicht zwangsweise funktionieren
*/

// Use a database sequence on id field
@Entity
@Table(name = "ADDRESS")
public class Address {
  @Id
  @SequenceGenerator(
     name = "address_seq",
     sequenceName = "address_seq",
     allocationSize = 1
  )
  @GeneratedValue(
     strategy = GenerationType.SEQUENCE,
     generator = "address_seq"
  )
  private long id;
}

// Delete dependent children, when the parent is going to be deleted (child-entites are orphans (=Waisen) then)
@OneToMany(mappedBy="foo", orphanRemoval=true)
private List<Bikes> bikes;

// Model a m:n relationship where the corresponding relationship record would be deleted when a entity record is deleted
@Entity
public class Team {
  @ManyToMany(cascade = { CascadeType.MERGE, CascadeType.PERSIST }, mappedBy="teams")
  private List<Match> matches;
}

@Entity
public class Match {
  @ManyToMany(cascade = { CascadeType.MERGE, CascadeType.PERSIST })
  @JoinTable(
    name="MATCH_TEAM",
    joinColumns={@JoinColumn(name="MATCH_ID", referencedColumnName="ID")},
    inverseJoinColumns={@JoinColumn(name="TEAM_ID", referencedColumnName="ID")}
  )
  private List<Team> teams;
}

// Remove Child Records, when the child record is set to null in the parents collection of children
// by setting "orphanRemoval = true"
@OneToOne(cascade = CascadeType.ALL, fetch=FetchType.LAZY, orphanRemoval = true)
private AvatarImage avatarImage;

// Mark children elements as "CascadeType.ALL" to refresh/delete/... them if the parent refreshes/deletes/...
// CascadeType.ALL contains PERSIST, REMOVE, REFRESH, MERGE, DETACH

// Several ways to delete records in the db
// http://www.objectdb.com/java/jpa/persistence/delete#Orphan_Removal

TODO: how to implemwnt equals and hashcode: https://vladmihalcea.com/2016/10/20/the-best-way-to-implement-equals-hashcode-and-tostring-with-jpa-and-hibernate/

Query Data
* JDBC - Plain SQL statements
* JPQL - Query language based on Enitity names (not on table names)
* Criteria Query - Build a query with a Java based API
* Native Query - Plain SQL statements

// Initialize DB schema in a Spring Boot App
resources/schema.sql

// Populate DB with data in a Spring Boot App
resources/data.sql

// Generic Rowmapper that maps all the columns to a Java Pojo
List<Person> persons = jdbcTemplate.query("select * from person", new BeanPropertyRowMapper<Person>(Person.class));

// Select a single object with JdbcTemplate
Person person = jdbcTemplate.queryForObject("select * from person where id=?", new Object[] { 12 }, new BeanPropertyRowMapper<Person>(Person.class));

// Delete a single object with JdbcTemplate
int rowsAffected = jdbcTemplate.update("delete * from person where id=?", new Object[] { 12 });

// Insert a single object with JdbcTemplate (java.sql.Timestamp)
int rowsAffected = jdbcTemplate.update("insert into person (id, name, birth_date) values (?, ?)", new Object[] { 12, "Meier", new Timestamp(new Date().getTime()) });

// Update a single object with JdbcTemplate
int rowsAffected = jdbcTemplate.update("update person set name=? where id=?", new Object[] { "Petersen", 12 });

EntityManger = Interface to the PersistenceContext

// Enable Transaction Management on each method
@Transactional // javax.persistence.Transactional
public class PersonJpaRepository {
  
  @PersistenceContext
  EntityManager entitiyManager;
  
  public Person findById(int id) {
  	return entityManager.find(Person.class, id);
  }
}

// Activate H2 Web-Console
spring.h2.console.enabled=true
  
// Show SQL Statements made with JPA / Hibernate
spring.jpa.show-sql=true

// Show the parameters of the statements
logging.level.org.hibernate.type=trace

// Pretty print the SQL printed on the console
spring.jpa.properties.hibernate.format_sql=true
  
// Show SQL statistics of JPA / Hibernate (e.g. how long did a query take)
spring.jpa.properties.hibernate.generate_statistics=true
logging.level.org.hibernate.stat=debug
  
// Find a JPA entity with EntityManager
Person person = entityManager.find(Person.class, id)
  
// Insert or Update a JPA entity with EntityManager
Person newPerson = entityManager.merge(person);

// Delete a JPA entity with EntityManager
entityManager.remove(person);

// Find all entities with EntityManager (using JPQL -> It uses entites in its query, not table names)
TypedQuery<Person> namedQuery = entityManager.createNamedQuery("find_all_persons", Person.class);
List<Person> persons = namedQuery.getResultList();

@Entity
@NamedQuery(name="find_all_persons", query="select p from Person p")
public class Person {
  ...
}

// Define multiple named queries on an entity
@Entity
@NamedQueries(
  value={
    @NamedQuery(name="find_all_persons", query="select p from Person p"),
    @NamedQuery(name="find_all_persons_with_a", query="select p from Person p where name like 'a%'")
  }
)
public class Person {
  ...
}

// Query the DB with a JPQL query
TypedQuery<Person> typedQuery = entityManager.createQuery("select p from Person p", Person.class);
List<Person> persons = typedQuery.getResultList();

// Reset In-Memory DB after a test method that changes the state of the DB
@Test
@DirtiesContext
public void deleteByIdBasic() {
  repository.deleteById(12);
  assertNull(repository.findById(12));
}

// Save an Entity with JPA
public Person save(Person person) {
  if (person.getId() == null) {
    // insert
    em.persist(person);
  } else {
    // update
    em.merge(person);
  }
  
  return person;
}

// In a @Transactional method every set-method results in an update of the row regardless of calling em.merge() on it
// The EntityManager keeps track of all the changes that are being made to an Entity
// In this case 1 x INSERT and 1 x UPDATE statement are being fired
@Transactional
public void foo() {
  Person person = new Person("Hansen");
  em.persist(person);
  person.setName("Meier");
}

// Send out changes to the DB within a Transaction
// In this case 1 x INSERT and 2 x UPDATE statements are being fired
@Transactional
public void foo() {
  Person person = new Person("Hansen");
  em.persist(person);
  em.flush();
  
  person.setName("Meier");
  em.flush();
  
  person.setName("Raab");
  em.flush();
}

// Prevent changes made to an Entity from going into the DB within an Transaction
// With em.detach(entity) the changes to this entity are no longer being tracked by the EntityManager
// In this case 1 x INSERT and 1 x Update statements are being fired
@Transactional
public void foo() {
  Person person = new Person("Hansen");
  em.persist(person);
  em.flush();
  
  person.setName("Meier");
  em.flush();
  
  em.detach(person);
  
  person.setName("Raab");
}

// Prevent changes made to all Entites from going into the DB within a Transaction
// With em.clear() the changes to all entites are no longer being tracked by the EntityManager
// In this case 2 x INSERT and no other statements are being fired
@Transactional
public void foo() {
  Person person = new Person("Hansen");
  em.persist(person);
  
  Person person2 = new Person("Meier");
  em.persist(person2);
  
  em.flush();
  em.clear();
  
  person.setName("Paulsen");
  em.flush();
  
  person2.setName("Raab");
  em.flush();
}

// Get a fresh copy of an Entity from the DB within a Transaction
// All changes that are not being flushed are now lost
@Transactional
public void foo() {
  Person person = new Person("Hansen");
  em.persist(person);
  
  Person person2 = new Person("Meier");
  em.persist(person2);
  
  em.flush();
  
  person.setName("Paulsen");  
  person2.setName("Raab");
  
  em.refresh(person);
  em.flush();
}

// Specify column properties
@Column(
  name="fullname", // Name of the table column - default: fieldname
  nullable=false, // Is the column nullable - default: true
  unique=true, // Is the value of this column unique across the table - default: false
  insertable=false, // Should this field be included in an INSERT command - default: true
  updateable=false, // Should this field be included in an UPDATE command - default: true
  length=20 // Maximum number of chars (only relevant to string fields) - default: 255
)
private String name;

// Automatically insert a timestamp when an entity was created (Hibernate specific)
@CreationTimestamp
private LocalDateTime createdDate;

// Automatically insert a timestamp when an entity was updated (Hibernate specific)
@UpdateTimestamp
private LocalDateTime lastUpdatedDate;

// Use NativeQuery to write plain SQL that is being executed directly
//
// Usecases for NativeQueries:
// * Performance tuning
// * DBMC specific features
// * Mass updates (JPA can only select a row and then update that row)
//
// NativeQueries doesn't use the PersistenceContext
// When you have some of the Entities that are being updated by a NativeQuery in your PersistenceContext you have to make sure to refresh() them.
Query nativeQuery = em.createNativeQuery("SELECT * FROM person", Person.class);
List<Person> persons = nativeQuery.getResultList();

// Use NativeQuery with positional parameters
Query nativeQuery = em.createNativeQuery("SELECT * FROM person WHERE id = ?", Person.class);
nativeQuery.setParameter(1, 1234);
List<Person> persons = nativeQuery.getResultList();

// Use NativeQuery with named parameters
Query nativeQuery = em.createNativeQuery("SELECT * FROM person WHERE id = :id", Person.class);
nativeQuery.setParameter("id", 1234);
List<Person> persons = nativeQuery.getResultList();

// Use NativeQuery for mass updates
Query nativeQuery = em.createNativeQuery("UPDATE person set last_updated_date=sysdate()");
int rowsAffected = nativeQuery.executeUpdate();

// Simple OneToOne relationship with one entity owning the relationship
//
// Fetch type of a @OneToOne relationship is "Eager" by default
// This means that the passwort table will be joined in the select statement to find the student
@Entity
public class Student {
  
  @OneToOne
  private Passport passport;
  
}
// -> SELECT * FROM student ... LEFT OUTER JOIN passport ON ...

// Set Fetch type to lazy so that the passport is only selected when it is needed
@Entity
public class Student {
  
  @OneToOne(fetch=FetchType.LAZY)
  private Passport passport;
  
}

Student student = em.find(Student.class, 1234); // -> SELECT * FROM student WHERE id=1234
String passportNumber = student.getPassport().getNumber(); // -> SELECT * FROM passport WHERE id=5678

// As soon as you introduce @Transactional a PersistenceContext will be created
// The PersistenceContext is a place where every Entity is being stored
// We interact with the PersistenceContext via an EntityManager (It is the interface to the PersistenceContext)
// The PersistenceContext is created in the start of a transaction and killed when the transaction is ended
// When there is no transaction, each method call to the EntityManager acts as an own transaction -> This is often the cause for a LazyInitializationException

Hibernate "Session" == Persistence Context

// Add mappedBy to the non-owning side of the relationship to get a biderectional navigation
@Entity
public class Passport {
  
  @OneToOne(fetch=FetchType.LAZY, mappedBy="passport")
  private Student student;
  
}

// Hibernate tries to make the SQL statements as late as possible
// In this case 1 x INSERT statement is being fired
@Transactional
public void createPerson() {
  em.persist(person1); // here the person gets its ID from the DB sequence, but the INSERT statement fires at the end of the transactional method
  person1.setName("Hans");
} // here does the INSERT happen and after that the transaction COMMIT

// On a 1:n relationship the relating entity reference has to be set on both sides
// But to add e.g. two new childs to a parent entity we only have to set the reference on both sides
// and then call persist() only on the childs
public void addReviewsForCourse() {
  Course course = em.find(Course.class, 1);

  Review goodReview = new Review("Super!");
  Review badReview = new Review("Bad!");

  course.addReview(goodReview);
  goodReview.setCourse(course);

  course.addReview(badReview);
  badReview.setCourse(course);

  em.persist(goodReview);
  em.persist(badReview);
}