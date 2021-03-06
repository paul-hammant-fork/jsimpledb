
/*
 * Copyright (C) 2015 Archie L. Cobbs. All rights reserved.
 */

package org.jsimpledb;

import com.google.common.primitives.Ints;

import java.util.List;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Optional;

import org.jsimpledb.annotation.FollowPath;
import org.jsimpledb.annotation.JSimpleClass;
import org.jsimpledb.test.TestSupport;
import org.jsimpledb.util.NavigableSets;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class FollowPathTest extends TestSupport {

    @Test
    public void testFollowPath() {
        final JSimpleDB jdb = BasicTest.getJSimpleDB(Family.class, Car.class, Bike.class, Dad.class, Mom.class, GoodChild.class);
        final JTransaction jtx = jdb.createTransaction(true, ValidationMode.AUTOMATIC);
        JTransaction.setCurrent(jtx);
        try {

            final Family family = jtx.create(Family.class);

            final Mom mom = jtx.create(Mom.class);
            final Dad dad = jtx.create(Dad.class);

            mom.setFamily(family);
            dad.setFamily(family);

            Assert.assertSame(family.getMom().get(), mom);
            Assert.assertSame(family.getDad().get(), dad);

            Assert.assertSame(mom.getHusband().get(), dad);
            Assert.assertSame(dad.getWife().get(), mom);

            TestSupport.checkSet(family.getMembers(), buildSet(mom, dad));
            TestSupport.checkSet(mom.getAllFamilyMembers(), buildSet(mom, dad));
            TestSupport.checkSet(dad.getAllFamilyMembers(), buildSet(mom, dad));

            final Child child1 = jtx.create(GoodChild.class);
            final Child child2 = jtx.create(GoodChild.class);
            final Child child3 = jtx.create(GoodChild.class);

            child1.setFamily(family);
            child2.setFamily(family);
            child3.setFamily(family);

            TestSupport.checkSet(family.getMembers(), buildSet(mom, dad, child1, child2, child3));
            TestSupport.checkSet(mom.getAllFamilyMembers(), buildSet(mom, dad, child1, child2, child3));
            TestSupport.checkSet(mom.getAllFamilyMembers(), family.getMembers());
            for (Person member : family.getMembers())
                TestSupport.checkSet(member.getAllFamilyMembers(), family.getMembers());

            TestSupport.checkSet(family.getChildren(), buildSet(child1, child2, child3));

            TestSupport.checkSet(child1.getSiblings(), family.getChildren());
            TestSupport.checkSet(child2.getSiblings(), family.getChildren());
            TestSupport.checkSet(child3.getSiblings(), family.getChildren());

            TestSupport.checkSet(child1.getSiblings2(), family.getChildren());
            TestSupport.checkSet(child2.getSiblings2(), family.getChildren());
            TestSupport.checkSet(child3.getSiblings2(), family.getChildren());

            final Car mcar = jtx.create(Car.class);
            final Car dcar = jtx.create(Car.class);

            mom.setVehicle(mcar);
            dad.setVehicle(dcar);

            final Bike bike1 = jtx.create(Bike.class);
            final Bike bike2 = jtx.create(Bike.class);
            final Bike bike3 = jtx.create(Bike.class);

            child1.setVehicle(bike1);
            child2.setVehicle(bike2);
            child3.setVehicle(bike2);

            TestSupport.checkSet(bike1.getAllOwners(), buildSet(child1));
            Assert.assertSame(bike1.getFirstOwner().get(), child1);

            TestSupport.checkSet(bike2.getAllOwners(), buildSet(child2, child3));
            Assert.assertSame(bike2.getFirstOwner().get(), child2.getObjId().compareTo(child3.getObjId()) < 0 ? child2 : child3);

            TestSupport.checkSet(bike3.getAllOwners(), buildSet());
            Assert.assertFalse(bike3.getFirstOwner().isPresent());

            Assert.assertSame(child1.getDadsVehicle().get(), dcar);
            Assert.assertSame(child2.getDadsVehicle().get(), dcar);
            Assert.assertSame(child3.getDadsVehicle().get(), dcar);

            Assert.assertSame(child1.getDadsVehicle2().get(), dcar);
            Assert.assertSame(child2.getDadsVehicle2().get(), dcar);
            Assert.assertSame(child3.getDadsVehicle2().get(), dcar);

            TestSupport.checkSet(child1.getSiblingBikes(), buildSet(bike1, bike2));
            TestSupport.checkSet(child2.getSiblingBikes(), buildSet(bike1, bike2));
            TestSupport.checkSet(child3.getSiblingBikes(), buildSet(bike1, bike2));

        } finally {
            JTransaction.setCurrent(null);
        }
    }

    @Test(dataProvider = "badChildClasses")
    public void testBadChild(Class<?> badChildClass) {
        try {
            BasicTest.getJSimpleDB(Family.class, Car.class, Bike.class, Dad.class, Mom.class, badChildClass);
            assert false;
        } catch (IllegalArgumentException e) {
            this.log.info("got expected " + e);
        }
    }

    @DataProvider(name = "badChildClasses")
    public Object[][] badChildClasses() {
        return new Object[][] {
            { BadChild1.class },
            { BadChild2.class },
            { BadChild3.class },
            { BadChild4.class },
            { BadChild5.class },
        };
    }

// Model Classes

    public interface Vehicle extends JObject {

        @FollowPath(inverseOf = "vehicle", startingFrom = Person.class)
        NavigableSet<Person> getAllOwners();

        @FollowPath(inverseOf = "vehicle", startingFrom = Person.class, firstOnly = true)
        Optional<Person> getFirstOwner();
    }

    public interface HasVehicle {
        Vehicle getVehicle();
        void setVehicle(Vehicle x);
    }

    public interface Person extends JObject {
        Family getFamily();
        void setFamily(Family x);

        @FollowPath(inverseOf = "family.^org.jsimpledb.FollowPathTest$Person:family^", startingFrom = Person.class)
        NavigableSet<Person> getAllFamilyMembers();
    }

    @JSimpleClass
    public abstract static class Family implements JObject {

        @FollowPath(startingFrom = Person.class, inverseOf = "family")
        public abstract NavigableSet<Person> getMembers();

        @FollowPath(value = "^Mom:family^", firstOnly = true)
        public abstract Optional<Mom> getMom();

        @FollowPath(value = "^Dad:family^", firstOnly = true)
        public abstract Optional<Dad> getDad();

        @FollowPath(startingFrom = Child.class, inverseOf = "family")
        public abstract NavigableSet<Child> getChildren();
    }

    @JSimpleClass
    public abstract static class Car implements Vehicle {
    }

    @JSimpleClass
    public abstract static class Bike implements Vehicle {
    }

    public interface Parent extends Person, HasVehicle {
    }

    @JSimpleClass
    public abstract static class Dad implements Parent {
        @FollowPath(value = "family.^Mom:family^", firstOnly = true)
        public abstract Optional<Parent> getWife();
    }

    @JSimpleClass
    public abstract static class Mom implements Parent {
        @FollowPath(value = "family.^Dad:family^", firstOnly = true)
        public abstract Optional<Parent> getHusband();
    }

    public abstract static class Child implements Person, HasVehicle {

        @FollowPath(value = "family.^org.jsimpledb.FollowPathTest$Child:family^.vehicle")
        public abstract NavigableSet<Vehicle> getSiblingBikes();

        @FollowPath(value = "family.^Dad:family^", firstOnly = true)
        public abstract Optional<Dad> getDad();

        @FollowPath(inverseOf = "family.^org.jsimpledb.FollowPathTest$Child:family^", startingFrom = Mom.class, firstOnly = true)
        public abstract Optional<Mom> getMom();

        @FollowPath("family.^org.jsimpledb.FollowPathTest$Child:family^")
        public abstract NavigableSet<Child> getSiblings();

        @FollowPath(startingFrom = Child.class, inverseOf = "family.^org.jsimpledb.FollowPathTest$Child:family^")
        public abstract NavigableSet<Child> getSiblings2();

        @FollowPath(value = "family.^Dad:family^.vehicle", firstOnly = true)
        public abstract Optional<Vehicle> getDadsVehicle();

        // Wider element type than necessary
        @FollowPath(value = "family.^Dad:family^.vehicle", firstOnly = true)
        public abstract Optional<JObject> getDadsVehicle2();
    }

    @JSimpleClass
    public abstract static class GoodChild extends Child {
    }

// Bad @FollowPath classes

    // Wrong return type - should be NavigableSet<Vehicle>
    @JSimpleClass
    public abstract static class BadChild1 extends Child {
        @FollowPath("family.vehicles.element")
        public abstract NavigableSet<Bike> getFamilyVehicles();
    }

    // Wrong return type - should be Optional<Vehicle>
    @JSimpleClass
    public abstract static class BadChild2 extends Child {
        @FollowPath(value = "family.vehicles.element", firstOnly = true)
        public abstract NavigableSet<Vehicle> getFirstFamilyVehicle();
    }

    // Wrong return type - should be Optional<Vehicle>
    @JSimpleClass
    public abstract static class BadChild3 extends Child {
        @FollowPath(value = "family.vehicles.element", firstOnly = true)
        public abstract Optional<Bike> getFirstFamilyVehicle();
    }

    // Invalid path not ending on BadChild4
    @JSimpleClass
    public abstract static class BadChild4 extends Child {
        @FollowPath(inverseOf = "vehicles.element", startingFrom = Parent.class)
        public abstract NavigableSet<Parent> getBogus();
    }

    // Invalid path
    @JSimpleClass
    public abstract static class BadChild5 extends Child {
        @FollowPath("foo.bar")
        public abstract NavigableSet<Object> getBogus();
    }
}
