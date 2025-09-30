package vn.edu.iuh.fit.services.pdf;

import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.List;
import com.lowagie.text.ListItem;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import vn.edu.iuh.fit.dtos.*;

import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Optional;

/**
 * Render hợp đồng dưới dạng PDF đơn giản với bố cục gồm thông tin các bên, tài sản và điều khoản thanh toán.
 */
@Component
public class ContractPdfRenderer {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final NumberFormat CURRENCY_FORMATTER = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));

    public byte[] render(ContractDto contract) {
        if (contract == null) {
            throw new IllegalArgumentException("contract is required");
        }

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4, 36, 36, 48, 48);
            PdfWriter.getInstance(document, out);
            document.open();

            VnFonts fonts = loadVnFonts();
            Font titleFont = fonts.title;
            Font subtitleFont = fonts.subtitle;
            Font normalFont = fonts.normal;

            addTitle(document, contract, titleFont, normalFont);
            addPartySection(document, contract, subtitleFont, normalFont);
            addGeneralTermsSection(document, subtitleFont, normalFont, contract);
            addPropertySection(document, contract, subtitleFont, normalFont);
            addPaymentSection(document, contract, subtitleFont, normalFont);
            addServiceTable(document, contract.getServices(), subtitleFont, normalFont);
            addNotesSection(document, contract.getNotes(), subtitleFont, normalFont);
            addSignatureSection(document, contract, subtitleFont, normalFont);

            document.close();
            return out.toByteArray();
        } catch (DocumentException | IOException ex) {
            throw new IllegalStateException("Unable to render contract PDF", ex);
        }
    }

    private void addTitle(Document document, ContractDto contract, Font titleFont, Font normalFont)
            throws DocumentException {
        Paragraph title = new Paragraph(Optional.ofNullable(contract.getTitle()).orElse("HỢP ĐỒNG THUÊ NHÀ"), titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        document.add(title);

        if (contract.getContractId() != null) {
            Paragraph code = new Paragraph("Mã hợp đồng: " + contract.getContractId(), normalFont);
            code.setAlignment(Element.ALIGN_CENTER);
            code.setSpacingBefore(4f);
            document.add(code);
        }

        document.add(Chunk.NEWLINE);
    }

    private void addPartySection(Document document, ContractDto contract, Font subtitleFont, Font normalFont)
            throws DocumentException {
        document.add(new Paragraph("1. Thông tin các bên", subtitleFont));

        BookingDto booking = contract.getBooking();
        UserDto landlord = booking != null && booking.getProperty() != null ? booking.getProperty().getLandlord() : null;
        UserDto tenant = booking != null ? booking.getTenant() : null;

        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setSpacingBefore(6f);
        table.setSpacingAfter(10f);

        table.addCell(createCell("Bên cho thuê (A)", subtitleFont));
        table.addCell(createCell("Bên thuê (B)", subtitleFont));

        table.addCell(createCell(formatPartyDetail(landlord), normalFont));
        table.addCell(createCell(formatTenantDetail(contract, tenant), normalFont));

        document.add(table);
    }

    private void addPropertySection(Document document, ContractDto contract, Font subtitleFont, Font normalFont)
            throws DocumentException {
        document.add(new Paragraph("2. Thông tin bất động sản", subtitleFont));

        PropertyDto property = contract.getBooking() != null ? contract.getBooking().getProperty() : null;
        AddressDto address = property != null ? property.getAddress() : null;

        StringBuilder sb = new StringBuilder();
        if (property != null) {
            sb.append("Tên toà nhà/phòng: ").append(optionalText(contract.getBuildingName(), property.getTitle())).append('\n');
            sb.append("Loại phòng: ").append(Optional.ofNullable(property.getPropertyType()).map(Enum::name).orElse("Không rõ")).append('\n');
        }

        if (address != null) {
            sb.append("Địa chỉ: ").append(buildAddress(address));
        }

        if (sb.length() == 0) {
            sb.append("Không có thông tin bất động sản");
        }

        Paragraph paragraph = new Paragraph(sb.toString(), normalFont);
        paragraph.setSpacingBefore(6f);
        paragraph.setSpacingAfter(10f);
        document.add(paragraph);
    }

    private void addPaymentSection(Document document, ContractDto contract, Font subtitleFont, Font normalFont)
            throws DocumentException {
        document.add(new Paragraph("4. Điều khoản thanh toán", subtitleFont));

        long numberOfNights = 0;
        if (contract.getStartDate() != null && contract.getEndDate() != null) {
            numberOfNights = java.time.temporal.ChronoUnit.DAYS.between(
                    contract.getStartDate(),
                    contract.getEndDate()
            );
            if (numberOfNights < 0) numberOfNights = 0; // tránh lỗi nếu dữ liệu ngược
        }
        BigDecimal total = BigDecimal.ZERO;
        if (contract.getRentPrice() != null && numberOfNights > 0) {
            total = contract.getRentPrice().multiply(BigDecimal.valueOf(numberOfNights));
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Giá thuê: ").append(formatMoney(contract.getRentPrice())).append('\n');
        sb.append("Số đêm: ").append(numberOfNights).append('\n');
        sb.append("Tổng tiền: ").append(formatMoney(total)).append('\n');
        sb.append("Ngày bắt đầu: ").append(formatDate(contract.getStartDate())).append('\n');
        sb.append("Ngày kết thúc: ").append(formatDate(contract.getEndDate())).append('\n');

        Paragraph paragraph = new Paragraph(sb.toString(), normalFont);
        paragraph.setSpacingBefore(6f);
        paragraph.setSpacingAfter(10f);
        document.add(paragraph);
    }

    private void addServiceTable(Document document, java.util.List<ContractServiceDto> services, Font subtitleFont, Font normalFont)
            throws DocumentException {
        document.add(new Paragraph("5. Dịch vụ đi kèm", subtitleFont));

        PdfPTable table = new PdfPTable(new float[]{3f, 1.5f, 1.5f, 3f});
        table.setWidthPercentage(100);
        table.setSpacingBefore(6f);
        table.setSpacingAfter(10f);

        table.addCell(createHeaderCell("Dịch vụ", normalFont));
        table.addCell(createHeaderCell("Phí", normalFont));
        table.addCell(createHeaderCell("Tính theo", normalFont));
        table.addCell(createHeaderCell("Ghi chú", normalFont));

        if (services != null && !services.isEmpty()) {
            for (ContractServiceDto service : services) {
                table.addCell(createCell(optionalText(service.getServiceName(), "N/A"), normalFont));
                table.addCell(createCell(service.getIsIncluded() != null && service.getIsIncluded()
                        ? "Đã bao gồm"
                        : formatMoney(service.getFee()), normalFont));
                table.addCell(createCell(Optional.ofNullable(service.getChargeBasis()).map(Enum::name).orElse("-"), normalFont));
                table.addCell(createCell(optionalText(service.getNote(), ""), normalFont));
            }
        } else {
            PdfPCell emptyCell = new PdfPCell(new Phrase("Không có dịch vụ bổ sung", normalFont));
            emptyCell.setColspan(4);
            table.addCell(emptyCell);
        }

        document.add(table);
    }

    private void addNotesSection(Document document, String notes, Font subtitleFont, Font normalFont)
            throws DocumentException {
        if (notes == null || notes.isBlank()) {
            return;
        }
        document.add(new Paragraph("6. Điều khoản bổ sung", subtitleFont));
        Paragraph paragraph = new Paragraph(notes, normalFont);
        paragraph.setSpacingBefore(6f);
        paragraph.setSpacingAfter(10f);
        document.add(paragraph);
    }

    private void addSignatureSection(Document document, ContractDto contract, Font subtitleFont, Font normalFont)
            throws DocumentException {
        document.add(new Paragraph("7. Chữ ký xác nhận", subtitleFont));

        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setSpacingBefore(20f);

        PdfPCell landlordCell = new PdfPCell(new Phrase("ĐẠI DIỆN BÊN A\n\n\n\n........................................", normalFont));
        landlordCell.setBorder(Rectangle.NO_BORDER);
        landlordCell.setHorizontalAlignment(Element.ALIGN_CENTER);

        PdfPCell tenantCell = new PdfPCell(new Phrase("ĐẠI DIỆN BÊN B\n\n\n\n........................................", normalFont));
        tenantCell.setBorder(Rectangle.NO_BORDER);
        tenantCell.setHorizontalAlignment(Element.ALIGN_CENTER);

        table.addCell(landlordCell);
        table.addCell(tenantCell);

        document.add(table);
    }

    private PdfPCell createCell(String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setPadding(8f);
        return cell;
    }

    private PdfPCell createHeaderCell(String text, Font font) {
        Font bold = new Font(font);
        bold.setStyle(Font.BOLD);
        PdfPCell cell = new PdfPCell(new Phrase(text, bold));
        cell.setPadding(8f);
        cell.setBackgroundColor(new Color(240, 240, 240));
        return cell;
    }

    private String formatPartyDetail(UserDto user) {
        if (user == null) {
            return "Tên: N/A\nĐiện thoại: -";
        }
        return "Tên: " + optionalText(user.getFullName(), "N/A") + '\n'
                + "Điện thoại: " + optionalText(user.getPhoneNumber(), "-") + '\n'
                + "Email: " + optionalText(user.getEmail(), "-");
    }

    private String formatTenantDetail(ContractDto contract, UserDto tenant) {
        StringBuilder sb = new StringBuilder();
        if (contract.getTenantName() != null) {
            sb.append("Tên: ").append(contract.getTenantName()).append('\n');
        } else {
            sb.append("Tên: ").append(optionalText(tenant != null ? tenant.getFullName() : null, "N/A")).append('\n');
        }
        sb.append("Điện thoại: ").append(optionalText(contract.getTenantPhone(), tenant != null ? tenant.getPhoneNumber() : null)).append('\n');
        if (tenant != null) {
            sb.append("Email: ").append(optionalText(tenant.getEmail(), "-")).append('\n');
        }
        if (contract.getTenantCccdFront() != null) {
            sb.append("CCCD mặt trước: ").append(contract.getTenantCccdFront()).append('\n');
        }
        if (contract.getTenantCccdBack() != null) {
            sb.append("CCCD mặt sau: ").append(contract.getTenantCccdBack());
        }
        return sb.toString();
    }

    private String optionalText(String value, String fallback) {
        return value != null && !value.isBlank() ? value : (fallback != null ? fallback : "-");
    }

    private String formatDate(LocalDate date) {
        return date != null ? date.format(DATE_FORMATTER) : "-";
    }

    private String formatMoney(BigDecimal value) {
        return value != null ? CURRENCY_FORMATTER.format(value) : "-";
    }

    private String buildAddress(AddressDto address) {
        if (address == null) {
            return "-";
        }
        if (address.getAddressFull() != null && !address.getAddressFull().isBlank()) {
            return address.getAddressFull();
        }

        java.util.ArrayList<String> parts = new java.util.ArrayList<>();
        addIfNotBlank(parts, address.getHouseNumber());
        addIfNotBlank(parts, address.getStreet());
        addIfNotBlank(parts, address.getWardName());
        addIfNotBlank(parts, address.getDistrictName());
        addIfNotBlank(parts, address.getProvinceName());

        return parts.isEmpty() ? "-" : String.join(", ", parts);
    }

    private void addIfNotBlank(java.util.List<String> parts, String value) {
        if (value != null && !value.isBlank()) {
            parts.add(value);
        }
    }
    private static class VnFonts {
        final Font title, subtitle, normal;
        VnFonts(Font t, Font s, Font n){ this.title=t; this.subtitle=s; this.normal=n; }
    }

    private VnFonts loadVnFonts() throws IOException, DocumentException {
        try (InputStream is = getClass().getResourceAsStream("/fonts/DejaVuSans.ttf")) {
            if (is == null) throw new IOException("Missing /fonts/DejaVuSans.ttf on classpath");
            byte[] bytes = is.readAllBytes();
            BaseFont bf = BaseFont.createFont(
                    "DejaVuSans.ttf", BaseFont.IDENTITY_H, BaseFont.EMBEDDED,  // Unicode + embed
                    false, bytes, null                                         // đọc từ byte[]
            );
            Font normal   = new Font(bf, 11);
            Font subtitle = new Font(bf, 13, Font.BOLD);
            Font title    = new Font(bf, 18, Font.BOLD);
            return new VnFonts(title, subtitle, normal);
        }
    }
    private void addGeneralTermsSection(Document document, Font subtitleFont, Font normalFont, ContractDto contract)
            throws DocumentException {
        document.add(new Paragraph("3. Điều khoản chung", subtitleFont));
        Paragraph intro = new Paragraph(
                "Các bên thống nhất thực hiện các điều khoản sau đây trong quá trình thuê và sử dụng tài sản:", normalFont);
        intro.setSpacingBefore(6f);
        intro.setSpacingAfter(6f);
        document.add(intro);

        List ol = new List(List.ORDERED);
        ol.setIndentationLeft(18f);

        ol.add(new ListItem("Bàn giao & kiểm kê tài sản: Các bên lập biên bản bàn giao mô tả hiện trạng, "
                + "danh mục trang thiết bị (nếu có). Trường hợp phát sinh thiếu/hư hỏng không có trong biên bản, "
                + "Bên B phải thông báo ngay cho Bên A trong vòng 24 giờ kể từ khi phát hiện.", normalFont));

        ol.add(new ListItem("Bảo quản & sử dụng: Bên B sử dụng căn hộ/phòng đúng mục đích, không tự ý cải tạo, "
                + "không gây ảnh hưởng đến kết cấu, hệ thống điện nước hoặc làm ảnh hưởng cư dân xung quanh.", normalFont));

        ol.add(new ListItem("Bảo trì & sửa chữa: Hư hỏng do hao mòn tự nhiên/hạ tầng chung do Bên A chịu trách nhiệm "
                + "sửa chữa; hư hỏng do lỗi sử dụng của Bên B thì Bên B chịu chi phí. Thời hạn phản hồi yêu cầu sửa chữa "
                + "của Bên A không quá 48 giờ kể từ khi nhận thông báo.", normalFont));

        ol.add(new ListItem("Bồi thường hư hại: Trường hợp tài sản/trang thiết bị bị hư hỏng, mất mát do lỗi Bên B, "
                + "Bên B bồi thường **theo thỏa thuận** dựa trên (i) chi phí sửa chữa/khôi phục thực tế hoặc (ii) "
                + "giá trị thay thế tại thời điểm phát sinh, ưu tiên có hóa đơn/chứng từ. Nếu không thống nhất được, "
                + "các bên có thể thuê đơn vị thẩm định độc lập; chi phí do bên có lỗi chịu.", normalFont));

        ol.add(new ListItem("Phí dịch vụ & tiện ích chung: Bên B có trách nhiệm thanh toán đầy đủ, đúng hạn các khoản "
                + "phí phát sinh theo quy định tòa nhà/ban quản lý (gửi xe, vệ sinh, rác, thang máy, an ninh...) nếu có.", normalFont));

        ol.add(new ListItem("Khách viếng thăm & an ninh: Bên B đảm bảo khách tuân thủ nội quy; chịu trách nhiệm "
                + "về hành vi, tài sản của khách trong thời gian lưu trú/viếng thăm.", normalFont));

        ol.add(new ListItem("Chấm dứt hợp đồng & thanh lý: Khi chấm dứt, các bên lập biên bản trả phòng, "
                + "đối chiếu công nợ/thiệt hại (nếu có) và thực hiện hoàn trả cọc theo điều khoản thanh toán.", normalFont));

        ol.add(new ListItem("Bảo mật thông tin: Thông tin hợp đồng và dữ liệu cá nhân chỉ sử dụng cho mục đích quản lý thuê; "
                + "không cung cấp cho bên thứ ba nếu không có sự đồng ý hoặc yêu cầu theo pháp luật.", normalFont));

        ol.add(new ListItem("Giải quyết tranh chấp: Ưu tiên thương lượng; nếu không đạt, tranh chấp được giải quyết "
                + "tại cơ quan có thẩm quyền theo pháp luật Việt Nam.", normalFont));

        document.add(ol);

        // Đẩy số thứ tự các mục sau lên +1 so với trước đó
        document.add(Chunk.NEWLINE);
    }
}
