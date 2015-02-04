xdata
=====
Xdata is a java library for storing and loading xdata files. The xdata file format 
was initially developed as an universal file format for the science fiction voxel 
game Xcylin (http://xcylin.com) but is suitable for a lot of other applications
as well. 

Xdata format highlights:

* separation of data model and data itself using keys
* typed and paramterized keys
* tree structure to organize data
* mapping of any type using the DataMarshaller interface
* support for lists of any type
* compression using standard gzip

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.moebiusgames/xdata/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.moebiusgames/xdata)

license
=======
Xdata is licensed under LGPL 2.1 and can therefore be used in any project, even
for commercial ones.

xdata file format
=================
The xdata file format stores information as so called data nodes. These nodes look for
example like this:

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
            |- _meta_classid                  = 0
            |---O check_dates: List (0)
            |---O build_date: DataNode
                |- timestamp                      = 1382191079972
                |- _meta_classid                  = 1
            |- wheels                         = 4
            |- horse_power                    = 180.5
    |- string                         = some car info

And with this data a class registry is persisted that contains the mapping from 
_meta_classid to the real class identifier. This helps later to look up the
correct marshaller to unmarshal the class.

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
xdata is available from the central maven repository, just use it like that:

    <dependency>
        <groupId>com.moebiusgames</groupId>
        <artifactId>xdata</artifactId>
        <version>1.4</version>
    </dependency>

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





