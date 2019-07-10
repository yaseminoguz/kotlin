// WITH_RUNTIME

interface I {
    var v: String
        @Synchronized get() = ""
        @Synchronized set(value) {}
}

class C : I

// TESTED_OBJECT_KIND: function
// TESTED_OBJECTS: I, getV
// FLAGS: ACC_PUBLIC, ACC_ABSTRACT

// TESTED_OBJECT_KIND: function
// TESTED_OBJECTS: I, setV
// FLAGS: ACC_PUBLIC, ACC_ABSTRACT

// TESTED_OBJECT_KIND: function
// TESTED_OBJECTS: I$DefaultImpls, getV
// FLAGS: ACC_PUBLIC, ACC_STATIC

// TESTED_OBJECT_KIND: function
// TESTED_OBJECTS: I$DefaultImpls, setV
// FLAGS: ACC_PUBLIC, ACC_STATIC

// TESTED_OBJECT_KIND: function
// TESTED_OBJECTS: C, getV
// FLAGS: ACC_PUBLIC, ACC_SYNCHRONIZED

// TESTED_OBJECT_KIND: function
// TESTED_OBJECTS: C, setV
// FLAGS: ACC_PUBLIC, ACC_SYNCHRONIZED
