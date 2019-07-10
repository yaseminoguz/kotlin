// WITH_RUNTIME

interface I {
    @Synchronized
    fun f() {}
}

class C : I

// TESTED_OBJECT_KIND: function
// TESTED_OBJECTS: I, f
// FLAGS: ACC_PUBLIC, ACC_ABSTRACT

// TESTED_OBJECT_KIND: function
// TESTED_OBJECTS: I$DefaultImpls, f
// FLAGS: ACC_PUBLIC, ACC_STATIC

// TESTED_OBJECT_KIND: function
// TESTED_OBJECTS: C, f
// FLAGS: ACC_PUBLIC, ACC_SYNCHRONIZED
