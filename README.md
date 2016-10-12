xdata
=====
xdata is a java library for storing and loading xdata files. The xdata file format 
was initially developed as an universal file format for the science fiction voxel 
game [Xcylin](http://xcylin.com) but is suitable for a lot of other applications
as well. 

xdata format highlights:

* separation of data model and data itself using keys
* typed and paramterized keys
* tree structure to organize data
* mapping of any type using the DataMarshaller interface
* support for lists of any type
* size of storable objects is only limited by memory
* compression using standard gzip

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.moebiusgames/xdata/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.moebiusgames/xdata)

license
=======
xdata is licensed under LGPL 2.1 and can therefore be used in any project, even
for commercial ones.

changelog
=========

* 1.6
  * Added new `EnumMarshaller`.
  * Added new `MapMarshaller` that supports all types of maps.
  * Added new possibility to create a Marshaller by adding a few annotations to a class and using the new `AnnotationBasedMarshaller`.
* 1.5
  * New way to use generic types in keys. This helps to get rid of a lot of ugly casting. See `DataKey.createKey(String name, GenericType<T> genType)` for more infos.
  * `GenericType<T>` can now also be used when specifying the type in a custom Marshaller. See the new `GenericDataMarshaller` class.
  * `XData.store()` and `XData.load()` now are recursion free (complete rewrite), so that we don't run into `StackOverflowExceptions` with big objects.
  * Also `XData.load()` now deserializes and unmarshalles objects on the fly and does not need to first construct the whole tree in memory before unmarshalling.
  * Objects that are used more than once within one persisted tree are now getting stored only once. All other occurances are replaced by references to the first one. Note that this feature does not work for recursive or cyclic dependencies of stored objects.

xdata file format
=================
The xdata file format stores information as so called data nodes (DataNode class). These 
nodes look for example like this:

    O Data Node
    |- short_key                      = 13
    |---O string_list_key: List (3)
        |-[0] abc
        |-[1] def
        |-[2] ghi
    |- byte_key                       = 5
    |- char_key                       = x
    |- int_key                        = 42
    |- string_key                     = helloworld!
    |- bool_key                       = true
    |- long_key                       = 786783647846876879
    |- double_key                     = 3.141592653589793
    |- float_key                      = 42.24

Each value is associated with a key (just like in a hash map). Like in relational
databases each key can have a default value and declare if it is nullable. 

Because xdata is stored as key-value pairs it effectivly separates the data schema
from the data and thus makes it portable and less problematic for downward compatability.

The xdata format itself can handle the following data types: String, Boolean, Byte, Char,
Short, Integer, Long, Float, Double, List and DataNode. Because DataNode itself is a 
supported type you can also build trees like this one:

    O Data Node
    |---O car_info: DataNode
        |- car                            = Car{wheels=4, horsePower=180.5, buildDate=Sat Oct 19 15:54:01 CEST 2013, checkDates=[]}
    |- string                         = some car info

As you can see by this example it is also possible to store complex types (here the Car) 
as well. To be as portable as possible there is no auto serialization process that serializes
the Car class in the file but rather in this case a CarMarshaller that maps
all the Cars data to a DataNode. The actual data that is stored looks like this:

    O Data Node
    |---O car_info: DataNode
        |---O car: DataNode
            |- _meta_classname                  = my.car
            |---O check_dates: List (0)
            |---O build_date: DataNode
                |- timestamp                      = 1382191079972
                |- _meta_classid                  = 1
            |- wheels                         = 4
            |- horse_power                    = 180.5
    |- string                         = some car info


So now you might ask how complex this marshaller looks like? Not very, because xdata
was design to give you as much power as possible while requiring you to write
as little code as possible. So lets look at that CarMarshaller:

    public class CarMarshaller implements DataMarshaller<Car> {

        private static final DataKey<Integer> KEY_WHEELS = DataKey.create("wheels", Integer.class);
        private static final DataKey<Float> KEY_HORSE_POWER = DataKey.create("horse_power", Float.class);
        private static final DataKey<Date> KEY_BUILD_DATE = DataKey.create("build_date", Date.class);
        private static final ListDataKey<Date> KEY_CHECK_DATES = ListDataKey.create("check_dates", Date.class);

        @Override
        public String getDataClassName() {
            return "xdata.test.car";
        }

        @Override
        public Class<Car> getDataClass() {
            return Car.class;
        }

        @Override
        public DataNode marshal(Car object) {
            DataNode node = new DataNode();
            node.setObject(KEY_WHEELS, object.getWheels());
            node.setObject(KEY_HORSE_POWER, object.getHorsePower());
            node.setObject(KEY_BUILD_DATE, object.getBuildDate());
            node.setObjectList(KEY_CHECK_DATES, object.getCheckDates());
            return node;
        }

        @Override
        public Car unMarshal(DataNode node) {
            final int wheels = node.getObject(KEY_WHEELS);
            final float horsePower = node.getObject(KEY_HORSE_POWER);
            final Date buildDate = node.getObject(KEY_BUILD_DATE);
            final List<Date> checkDates = node.getObjectList(KEY_CHECK_DATES);
            Car car = new Car(wheels, horsePower, buildDate);
            car.setCheckDates(checkDates);
            return car;
        }

        @Override
        public List<DataMarshaller<?>> getRequiredMarshallers() {
            List<DataMarshaller<?>> list = new ArrayList<DataMarshaller<?>>();
            list.add(new DateMarshaller());
            return list;
        }

    }

As you see the important methods are only marshal() and unMarshal() - both are only
mapping data to nodes and back. The method getRequiredMarshallers() tells XData 
that also the DateMarshaller is required, because the car stores some date
information.

On top of all these features of the file format, xdata is also gz compressed which
helps to make it small but also keeps it compatible.

how to use it?
==============
xdata is available from the central maven repository, just click this link
to get to the most recent version:

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.moebiusgames/xdata/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.moebiusgames/xdata)

example
=======
To serialize some data to a xdata file just do the following:

    //define some keys:
    final static DataKey<String> MY_KEY = DataKey.create("mykey", String.class);
    //...
    DataNode node = new DataNode();
    node.setObject(MY_KEY, "hello world");
    XData.store(node, new File("somefile.xdata"));
    //...
    //now restore the data again
    DataNode restoredNode = XData.load(new File("somefile.xdata"));
    //do sth with the data in the node e.g.
    System.out.println(node.getObject(MY_KEY));

A more sophisticated example would be to implement the DataMarshaller and
just put your objects directly into a data node. Check out the included
marshallers in this package to learn how to write your own marshallers:

    https://github.com/entrusc/xdata/tree/master/src/main/java/com/moebiusgames/xdata/marshaller





