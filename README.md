**NOTE: This library is still in beta!**
 
#Moshi: Pristine Models [![Build Status](https://travis-ci.org/jayrave/moshi-pristine-models.svg?branch=master)](https://travis-ci.org/jayrave/moshi-pristine-models) 
This is an add-on to [Moshi](https://github.com/square/moshi) which allows
 - to programmatically declare the mapping between models & JSON
 - to keep your models pristine => free of annotations & only concerned about the business logic
  
It is pretty easy to define the mappings! The following method is useful in case where the models are written in Kotlin
```
// Model
data class User(val name: String, val age: Int)

// Mapping from/to JSON
class UserMapper : Mapper<User>() {
    val name = field(User::name, "user_name")
    val age = field(User::age, "user_age")
    
    override fun create(value: Value<User>) = User(value of name, value of age)
}
```

The following method is useful in case the models are written in Java or Kotlin (with private properties) 
 
```
// Model
class User {
    private final String name;
    private final int age;
    
    public User(String name, int age) {
        this.name = name;
        this.age = age;
    }
    
    public String getName() {
        return name;
    }
    
    public int getAge() {
        return age;
    }
}

```
```
// Mapping from/to JSON
class UserMapper : Mapper<User>() {
    val name = field("user_field", false, object : PropertyExtractor<User, String> {
        override val type: Type = String::class.javaObjectType
        override fun extractFrom(t: User): String = t.getName()
    })

    val age = field("user_age", false, object : PropertyExtractor<User, Int> {
        override val type: Type = Int::class.javaPrimitiveType!!
        override fun extractFrom(t: User): Int = t.getAge()
    })

    override fun create(value: Value<User>): User {
        return User(value of name, value of age)
    }
}
```

Both of the above mentioned way of defining the mapping can map the User model from/to this JSON
```
{
    "user_name": "John Muir", 
    "user_age": 76
}
```

Once the mappings have been defined, `Moshi` has to be taught about these
```
val factory = PristineModelsJsonAdapterFactory.Builder()
        .add(User::class.java, UserMapper())
        .build()
        
val moshi = Moshi.Builder()
        .add(factory)
        // anything else you wanna teach moshi about
        .build()
```

Voila! Now `Moshi` knows how to handle `User`