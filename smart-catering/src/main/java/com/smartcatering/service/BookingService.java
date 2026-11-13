package com.smartcatering.service;

import com.smartcatering.dto.Dtos.*;
import com.smartcatering.entity.*;
import com.smartcatering.enums.*;
import com.smartcatering.exception.ApiException;
import com.smartcatering.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BookingService {
    private final BookingRepository bookingRepo;
    private final BookingItemRepository itemRepo;
    private final UserRepository userRepo;

    static final double WAITER_RATE = 1000;
    static final double CAPTAIN_RATE = 1800;
    static final double ADVANCE_PERCENT = 0.30;

    // ---------- 1. Customer booking banata hai ----------
    @Transactional
    public Booking create(User customer, BookingRequest r) {
        if (r.eventDate().isBefore(LocalDate.now()))
            throw new ApiException("Purani date par booking nahi ho sakti");

        Booking b = new Booking();
        b.setCustomer(customer);
        b.setEventDate(r.eventDate());
        b.setShift(r.shift());
        b.setPlates(r.plates() == null ? 0 : r.plates());

        double total = 0;
        for (ItemRequest ir : r.items()) {
            BookingItem it = buildItem(b, customer, ir, b.getPlates(), true);
            b.getItems().add(it);
            total += it.getAmount();
        }
        b.setTotalAmount(total);
        return bookingRepo.save(b);
    }

    // ---------- 2. Vendor accept / reject karta hai ----------
    @Transactional
    public BookingItem respond(User vendor, Long itemId, boolean accept) {
        BookingItem it = itemRepo.findById(itemId).orElseThrow(() -> new ApiException("Request nahi mili"));
        if (!it.getVendor().getId().equals(vendor.getId()))
            throw new ApiException("Ye request aapki nahi hai");
        if (it.getStatus() != ItemStatus.PENDING)
            throw new ApiException("Is request par aap pehle hi reply de chuke ho");

        Booking b = it.getBooking();
        if (b.getStatus() != BookingStatus.PENDING)
            throw new ApiException("Ye booking ab pending nahi hai");

        if (accept) {
            boolean subPending = b.getItems().stream()
                    .anyMatch(x -> !x.isChargedToCustomer()
                            && x.getRequestedBy().getId().equals(vendor.getId())
                            && x.getStatus() == ItemStatus.PENDING);
            if (subPending) throw new ApiException("Pehle apne sub-vendors ke reply ka wait karo");
            it.setStatus(ItemStatus.ACCEPTED);
        } else {
            it.setStatus(ItemStatus.REJECTED);
            if (it.isChargedToCustomer()) {
                b.setStatus(BookingStatus.REJECTED);
                b.getItems().stream()
                        .filter(x -> ItemStatus.ACTIVE.contains(x.getStatus()))
                        .forEach(x -> x.setStatus(ItemStatus.CANCELLED));
            }
        }

        if (b.getStatus() == BookingStatus.PENDING) {
            boolean allAccepted = b.getItems().stream()
                    .filter(x -> x.getStatus() != ItemStatus.REJECTED)
                    .allMatch(x -> x.getStatus() == ItemStatus.ACCEPTED);
            if (allAccepted) b.setStatus(BookingStatus.CONFIRMED);
        }
        bookingRepo.save(b);
        return it;
    }

    // ---------- 3. Caterer sub-booking ----------
    @Transactional
    public BookingItem subBook(User caterer, Long parentItemId, ItemRequest ir) {
        BookingItem parent = itemRepo.findById(parentItemId)
                .orElseThrow(() -> new ApiException("Request nahi mili"));
        if (!parent.getVendor().getId().equals(caterer.getId()) || caterer.getRole() != Role.CATERER)
            throw new ApiException("Sirf is booking ka caterer sub-booking kar sakta hai");
        if (parent.getStatus() != ItemStatus.PENDING)
            throw new ApiException("Sub-booking sirf accept karne se pehle ho sakti hai");

        Booking b = parent.getBooking();
        BookingItem it = buildItem(b, caterer, ir, b.getPlates(), false);
        b.getItems().add(it);
        bookingRepo.save(b);
        return it;
    }

    // ---------- 4. Payments (sirf record) ----------
    @Transactional
    public Booking payAdvance(User customer, Long bookingId) {
        Booking b = ownBooking(customer, bookingId);
        if (b.getStatus() == BookingStatus.REJECTED) throw new ApiException("Booking reject ho chuki hai");
        if (b.getAdvancePaid() > 0) throw new ApiException("Advance pehle hi pay ho chuka hai");
        b.setAdvancePaid(Math.round(b.getTotalAmount() * ADVANCE_PERCENT));
        return bookingRepo.save(b);
    }

    @Transactional
    public Booking payBalance(User customer, Long bookingId) {
        Booking b = ownBooking(customer, bookingId);
        if (b.getStatus() != BookingStatus.CONFIRMED) throw new ApiException("Booking abhi confirm nahi hui");
        if (b.getAdvancePaid() == 0) throw new ApiException("Pehle advance pay karo");
        if (b.isBalancePaid()) throw new ApiException("Balance pehle hi pay ho chuka hai");
        if (b.getEventDate().isAfter(LocalDate.now()))
            throw new ApiException("Balance function wale din hi pay hoga");
        b.setBalancePaid(true);
        b.setStatus(BookingStatus.COMPLETED);
        return bookingRepo.save(b);
    }

    // ---------- 5. Dekhne ke liye ----------
    public List<Booking> myBookings(User customer) {
        return bookingRepo.findByCustomerIdOrderByEventDateDesc(customer.getId());
    }

    public List<RequestView> requestsFor(User vendor) {
        return itemRepo.findByVendorIdOrderByIdDesc(vendor.getId()).stream()
                .map(i -> new RequestView(i.getId(), i.getBooking().getId(),
                        i.getBooking().getEventDate(), i.getBooking().getShift(),
                        i.getBooking().getPlates(), i.getWaiters(), i.getCaptains(),
                        i.getAmount(), i.getStatus(), i.getRequestedBy().getName(),
                        !i.isChargedToCustomer()))
                .toList();
    }

    // ---------- Helpers ----------
    private Booking ownBooking(User customer, Long id) {
        Booking b = bookingRepo.findById(id).orElseThrow(() -> new ApiException("Booking nahi mili"));
        if (!b.getCustomer().getId().equals(customer.getId()))
            throw new ApiException("Ye booking aapki nahi hai");
        return b;
    }

    private BookingItem buildItem(Booking b, User by, ItemRequest ir, int plates, boolean charged) {
        User v = userRepo.findById(ir.vendorId()).orElseThrow(() -> new ApiException("Vendor nahi mila"));
        if (v.getRole() == Role.CUSTOMER) throw new ApiException("Customer ko book nahi kar sakte");
        if (!charged && v.getRole() == Role.CATERER)
            throw new ApiException("Caterer dusre caterer ko sub-book nahi kar sakta");

        int w = ir.waiters() == null ? 0 : ir.waiters();
        int c = ir.captains() == null ? 0 : ir.captains();
        LocalDate d = b.getEventDate();
        Shift s = b.getShift();
        double amount;

        if (v.getRole() == Role.THEKEDAR) {
            if (w + c <= 0) throw new ApiException("Kam se kam 1 waiter ya captain chahiye");
            long wFree = v.getWaiterCount() - itemRepo.waitersBooked(v.getId(), d, s, ItemStatus.ACTIVE);
            long cFree = v.getCaptainCount() - itemRepo.captainsBooked(v.getId(), d, s, ItemStatus.ACTIVE);
            if (w > wFree || c > cFree)
                throw new ApiException(v.getName() + " ke paas itne free nahi hain (waiter free: "
                        + wFree + ", captain free: " + cFree + ")");
            amount = w * WAITER_RATE + c * CAPTAIN_RATE;
        } else {
            if (itemRepo.isBooked(v.getId(), d, s, ItemStatus.ACTIVE))
                throw new ApiException(v.getName() + " us date/shift me available nahi hai");
            if (v.getBasePrice() == null)
                throw new ApiException(v.getName() + " ne abhi price set nahi kiya");
            if (v.getRole() == Role.CATERER) {
                if (plates <= 0) throw new ApiException("Caterer ke liye plates daalna zaruri hai");
                amount = v.getBasePrice() * plates;
            } else {
                amount = v.getBasePrice();
            }
        }

        BookingItem it = new BookingItem();
        it.setBooking(b);
        it.setVendor(v);
        it.setRequestedBy(by);
        it.setWaiters(w);
        it.setCaptains(c);
        it.setAmount(amount);
        it.setChargedToCustomer(charged);
        return it;
    }
}