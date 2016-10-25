**NOTE: This library is still in beta!**
 
#Moshi: Pristine Models [![Build Status](https://travis-ci.org/jayrave/moshi-pristine-models.svg?branch=master)](https://travis-ci.org/jayrave/moshi-pristine-models) [ ![Download](https://api.bintray.com/packages/jayrave/kotlin/moshi-pristine-models/images/download.svg) ](https://bintray.com/jayrave/kotlin/moshi-pristine-models/_latestVersion) 
This is an add-on to [Moshi](https://github.com/square/moshi) which allows
 - to programmatically define mapping between models & JSON
 - to keep your models pristine => free of annotations & only concerned about the business logic
  
It is pretty easy to define the mappings!
```kotlin
data class User(val name: String, val age: Int)
```
```kotlin
class UserMapper : Mapper<User>() {
    val name = field(User::name, "user_name")
    val age = field(User::age, "user_age")
    
    override fun create(value: Value<User>) = User(value of name, value of age)
}
```

Once the mappings have been defined, `Moshi` has to be taught about these
```kotlin
val factory = PristineModelsJsonAdapterFactory.Builder()
        .add(User::class.java, UserMapper())
        .build()
        
val moshi = Moshi.Builder()
        .add(factory)
        // anything else you wanna teach moshi about
        .build()
```

Voila! Now `Moshi` knows how to map `User` to/from this JSON
```javascript
{
    "user_name": "John Muir", 
    "user_age": 76
}
```

This raises some questions:
 - What if the models are written in `Java`?
 - What if the models have private properties?
 
Well, there is a way to handle those situations too (considering there are public getters & and a public constructor). Let us consider that the same `User` model we saw above is now written in `Java`. The mapping is a little bit more involved, but nevertheless possible
```java
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
```kotlin
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

##Download
```gradle
// This is usually in the top-level build.gradle file
allprojects {
    repositories {
        jcenter() // Since the JAR lives in Bintray's jCenter
    }
}

dependencies {
    compile "com.jayrave:moshi-pristine-models:$version"
}
```

##Check this out
If you like keeping your models clean, you may be interested in checking out another library => [Falkon](https://github.com/jayrave/falkon) (Disclaimer: I am the author), which helps to keep your models free of database/ORM specific annotations. Like this library, Falkon also enables to programmatically define the mapping between models & database records 
