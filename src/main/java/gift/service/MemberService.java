package gift.service;

import gift.constants.ErrorMessage;
import gift.dto.Member;
import gift.dto.Product;
import gift.dto.Wishlist;
import gift.jwt.JwtUtil;
import gift.repository.MemberJpaDao;
import gift.repository.WishlistJpaDao;
import java.util.List;
import java.util.NoSuchElementException;
import org.springframework.stereotype.Service;

@Service
public class MemberService {

    private final MemberJpaDao memberJpaDao;
    private final WishlistJpaDao wishlistJpaDao;
    private final JwtUtil jwtUtil;

    public MemberService(MemberJpaDao memberJpaDao, WishlistJpaDao wishlistJpaDao,
        JwtUtil jwtUtil) {
        this.memberJpaDao = memberJpaDao;
        this.wishlistJpaDao = wishlistJpaDao;
        this.jwtUtil = jwtUtil;
    }

    /**
     * 회원 가입. <br> 이미 존재하는 email 이면, IllegalArgumentException
     *
     * @param member
     */
    public void registerMember(Member member) {
        memberJpaDao.findByEmail(member.getEmail())
            .ifPresent(user -> {
                throw new IllegalArgumentException(ErrorMessage.EMAIL_ALREADY_EXISTS_MSG);
            });
        memberJpaDao.save(member);
    }

    /**
     * 로그인. <br> email이 일치하지 않으면 NoSuchElementException <br> password가 일치하지 않으면
     * IllegalArgumentException
     *
     * @param member
     * @return
     */
    public String login(Member member) {
        Member queriedMember = memberJpaDao.findByEmail(member.getEmail())
            .orElseThrow(() -> new NoSuchElementException(ErrorMessage.MEMBER_NOT_EXISTS_MSG));
        if (!queriedMember.isCorrectPassword(member.getPassword())) {
            throw new IllegalArgumentException(ErrorMessage.INVALID_PASSWORD_MSG);
        }
        return jwtUtil.createJwt(member.getEmail(), 1000 * 60 * 30);
    }

    /**
     * 회원의 위시 리스트 조회
     *
     * @param email
     * @return
     */
    public List<Product> getAllWishlist(String email) {
        return wishlistJpaDao.findAllWishlistByEmail(email);
    }

    /**
     * 위시 리스트에 상품 추가
     *
     * @param wish
     */
    public void addWishlist(Wishlist wish) {
        wishlistJpaDao.findByEmailAndProductId(wish.getEmail(), wish.getProductId())
            .ifPresent(v -> {
                throw new IllegalArgumentException(ErrorMessage.WISHLIST_ALREADY_EXISTS_MSG);
            });
        wishlistJpaDao.save(wish);
    }

    /**
     * 위시 리스트에서 상품 삭제
     *
     * @param wish
     */
    public void deleteWishlist(Wishlist wish) {
        wishlistJpaDao.findByEmailAndProductId(wish.getEmail(), wish.getProductId())
            .orElseThrow(() -> new NoSuchElementException(ErrorMessage.WISHLIST_NOT_EXISTS_MSG));
        wishlistJpaDao.deleteByEmailAndProductId(wish.getEmail(), wish.getProductId());
    }
}
