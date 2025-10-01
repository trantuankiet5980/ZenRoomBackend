package vn.edu.iuh.fit.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;
import vn.edu.iuh.fit.dtos.BookingDto;
import vn.edu.iuh.fit.dtos.requests.BookingCreateRequest;
import vn.edu.iuh.fit.entities.enums.BookingStatus;
import vn.edu.iuh.fit.mappers.BookingMapper;
import vn.edu.iuh.fit.repositories.BookingRepository;
import vn.edu.iuh.fit.services.BookingService;

import java.security.Principal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/bookings")
@RequiredArgsConstructor
public class BookingController {
    private final BookingService bookingService;
    private final BookingRepository bookingRepo;
    private final BookingMapper bookingMapper;

    @PostMapping
    public BookingDto create(@RequestBody BookingCreateRequest req, Principal principal) {
        return bookingService.createDaily(principal.getName(), req);
    }

    @PostMapping("/{bookingId}/approve")
    public BookingDto approve(@PathVariable String bookingId, Principal principal) {
        return bookingService.approve(bookingId, principal.getName());
    }

    @PostMapping("/{bookingId}/cancel")
    public BookingDto cancel(@PathVariable String bookingId, Principal principal) {
        return bookingService.cancel(bookingId, principal.getName());
    }

    @PostMapping("/{bookingId}/check-in")
    public BookingDto checkIn(@PathVariable String bookingId, Principal principal) {
        return bookingService.checkIn(bookingId, principal.getName());
    }

    @PostMapping("/{bookingId}/check-out")
    public BookingDto checkOut(@PathVariable String bookingId, Principal principal) {
        return bookingService.checkOut(bookingId, principal.getName());
    }

    @GetMapping("/me")
    public Page<BookingDto> myBookings(Principal principal,
                                       @RequestParam(defaultValue = "0") int page,
                                       @RequestParam(defaultValue = "10") int size) {
        return bookingRepo.findByTenant_UserIdOrderByCreatedAtDesc(
                principal.getName(), PageRequest.of(page, size)).map(bookingMapper::toDto);
    }

    @GetMapping("/landlord")
    public Page<BookingDto> landlordBookings(Principal principal,
                                             @RequestParam(defaultValue = "0") int page,
                                             @RequestParam(defaultValue = "10") int size) {
        return bookingRepo.findByProperty_Landlord_UserIdOrderByCreatedAtDesc(
                principal.getName(), PageRequest.of(page, size)).map(bookingMapper::toDto);
    }

    @GetMapping("/{bookingId}")
    public BookingDto getOne(@PathVariable String bookingId, Principal principal) {
        return bookingService.getOne(bookingId, principal.getName());
    }

    @GetMapping("/property/{propertyId}/booked-dates")
    public List<LocalDate> getBookedDates(@PathVariable String propertyId) {
        return bookingService.getBookedDates(propertyId);
    }

    @GetMapping("/me/pending")
    public Page<BookingDto> myPendingBookings(Principal principal,
                                              @RequestParam(defaultValue = "0") int page,
                                              @RequestParam(defaultValue = "10") int size) {
        return bookingRepo.findByTenant_UserIdAndBookingStatusOrderByCreatedAtDesc(
                        principal.getName(), BookingStatus.PENDING_PAYMENT, PageRequest.of(page, size))
                .map(bookingMapper::toDto);
    }

    @GetMapping("/me/approved")
    public Page<BookingDto> myApprovedBookings(Principal principal,
                                               @RequestParam(defaultValue = "0") int page,
                                               @RequestParam(defaultValue = "10") int size) {
        return bookingRepo.findByTenant_UserIdAndBookingStatusOrderByCreatedAtDesc(
                        principal.getName(), BookingStatus.APPROVED, PageRequest.of(page, size))
                .map(bookingMapper::toDto);
    }

    @GetMapping("/landlord/pending")
    public Page<BookingDto> landlordPendingBookings(Principal principal,
                                                    @RequestParam(defaultValue = "0") int page,
                                                    @RequestParam(defaultValue = "10") int size) {
        return bookingRepo.findByProperty_Landlord_UserIdAndBookingStatusOrderByCreatedAtDesc(
                        principal.getName(), BookingStatus.AWAITING_LANDLORD_APPROVAL, PageRequest.of(page, size))
                .map(bookingMapper::toDto);
    }

}
