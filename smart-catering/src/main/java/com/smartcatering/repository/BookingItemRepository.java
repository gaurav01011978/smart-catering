package com.smartcatering.repository;

import com.smartcatering.entity.BookingItem;
import com.smartcatering.enums.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.List;

public interface BookingItemRepository extends JpaRepository<BookingItem, Long> {

    List<BookingItem> findByVendorIdOrderByIdDesc(Long vendorId);

    // Kya vendor us date+shift me already booked hai?
    @Query("select count(i) > 0 from BookingItem i where i.vendor.id = :vid " +
           "and i.booking.eventDate = :d and i.booking.shift = :s and i.status in :st")
    boolean isBooked(@Param("vid") Long vid, @Param("d") LocalDate d,
                     @Param("s") Shift s, @Param("st") List<ItemStatus> st);

    // Thekedar ke kitne waiter/captain us shift me pehle se booked hain
    @Query("select coalesce(sum(i.waiters), 0) from BookingItem i where i.vendor.id = :vid " +
           "and i.booking.eventDate = :d and i.booking.shift = :s and i.status in :st")
    long waitersBooked(@Param("vid") Long vid, @Param("d") LocalDate d,
                       @Param("s") Shift s, @Param("st") List<ItemStatus> st);

    @Query("select coalesce(sum(i.captains), 0) from BookingItem i where i.vendor.id = :vid " +
           "and i.booking.eventDate = :d and i.booking.shift = :s and i.status in :st")
    long captainsBooked(@Param("vid") Long vid, @Param("d") LocalDate d,
                        @Param("s") Shift s, @Param("st") List<ItemStatus> st);
}