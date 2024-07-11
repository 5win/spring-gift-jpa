package gift.service;

import gift.constants.ErrorMessage;
import gift.dto.Member;
import gift.dto.Product;
import gift.dto.ProductDto;
import gift.dto.Wishlist;
import gift.dto.WishlistRequest;
import gift.jwt.JwtUtil;
import gift.repository.MemberJpaDao;
import gift.repository.ProductJpaDao;
import gift.repository.WishlistJpaDao;
import java.util.List;
import java.util.NoSuchElementException;
import org.springframework.stereotype.Service;

@Service
public class MemberService {

    private final MemberJpaDao memberJpaDao;
    private final WishlistJpaDao wishlistJpaDao;
    private final ProductJpaDao productJpaDao;
    private final JwtUtil jwtUtil;

    public MemberService(MemberJpaDao memberJpaDao, WishlistJpaDao wishlistJpaDao,
        ProductJpaDao productJpaDao, JwtUtil jwtUtil) {
        this.memberJpaDao = memberJpaDao;
        this.wishlistJpaDao = wishlistJpaDao;
        this.productJpaDao = productJpaDao;
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
    public List<ProductDto> getAllWishlist(String email) {
        Member member = memberJpaDao.findByEmail(email)
            .orElseThrow(() -> new NoSuchElementException(ErrorMessage.MEMBER_NOT_EXISTS_MSG));
        return member.getWishlist().stream().map(o -> new ProductDto(o.getProduct())).toList();
    }

    /**
     * 위시 리스트에 상품 추가
     *
     * @param wish
     */
    public void addWishlist(WishlistRequest wish) {
        Wishlist wishlist = convertWishlistRequestToWishlist(wish);

        wishlist.getMember().getWishlist().add(wishlist);
        wishlist.getProduct().getWishlist().add(wishlist);
        wishlistJpaDao.findByWishlist(wishlist)
            .ifPresent(v -> {
                throw new IllegalArgumentException(ErrorMessage.WISHLIST_ALREADY_EXISTS_MSG);
            });
        wishlistJpaDao.save(wishlist);
    }

    /**
     * 위시 리스트에서 상품 삭제
     *
     * @param wish
     */
    public void deleteWishlist(WishlistRequest wish) {
        Wishlist deleteWish = convertWishlistRequestToWishlist(wish);

        wishlistJpaDao.findByWishlist(deleteWish)
            .orElseThrow(() -> new NoSuchElementException(ErrorMessage.WISHLIST_NOT_EXISTS_MSG));
        wishlistJpaDao.deleteByWishlist(deleteWish);
    }

    /**
     * WishlistRequest를 Wishlist Entity로 변환
     */
    private Wishlist convertWishlistRequestToWishlist(WishlistRequest wish) {
        Member member = memberJpaDao.findByEmail(wish.getEmail())
            .orElseThrow(() -> new NoSuchElementException(ErrorMessage.MEMBER_NOT_EXISTS_MSG));
        Product product = productJpaDao.findById(wish.getProductId())
            .orElseThrow(() -> new NoSuchElementException(ErrorMessage.PRODUCT_NOT_EXISTS_MSG));
        return new Wishlist(member, product);
    }
}
