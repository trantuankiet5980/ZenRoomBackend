package vn.edu.iuh.fit.mappers;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import vn.edu.iuh.fit.dtos.ReviewReplyDto;
import vn.edu.iuh.fit.entities.ReviewReply;

@Component
@RequiredArgsConstructor
public class ReviewReplyMapper {
    private final ReviewMapper reviewMapper;
    private final UserMapper userMapper;

    /** Entity -> DTO */
    public ReviewReplyDto toDto(vn.edu.iuh.fit.entities.ReviewReply e) {
        if (e == null) return null;
        return new vn.edu.iuh.fit.dtos.ReviewReplyDto(
                e.getReplyId(),
                reviewMapper.toDto(e.getReview()),
                userMapper.toDto(e.getLandlord()),
                e.getReplyText(),
                e.getCreatedAt(),
                e.getUpdatedAt()
        );
    }

    /** DTO -> Entity */
    public ReviewReply toEntity(vn.edu.iuh.fit.dtos.ReviewReplyDto d) {
        if (d == null) return null;
        return vn.edu.iuh.fit.entities.ReviewReply.builder()
                .replyId(d.getReplyId())
                .replyText(d.getReplyText())
                .createdAt(d.getCreatedAt())
                .updatedAt(d.getUpdatedAt())
                .build();
    }

}
