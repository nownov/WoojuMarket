package com.mysite.wooju.sale;

import com.mysite.wooju.category.Category;
import com.mysite.wooju.DataNotFoundException;
import com.mysite.wooju.answer.Answer;
import com.mysite.wooju.user.SiteUser;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@Service
public class SaleService {

	private final SaleRepository saleRepository;

	private Specification<Sale> search(String kw) {
		return new Specification<>() {
			private static final long serialVersionUID = 1L;

			@Override
			public Predicate toPredicate(Root<Sale> q, CriteriaQuery<?> query, CriteriaBuilder cb) {
				query.distinct(true);
				Join<Sale, SiteUser> u1 = q.join("author", JoinType.LEFT);
				Join<Sale, Answer> a = q.join("answerList", JoinType.LEFT);
				Join<Answer, SiteUser> u2 = a.join("author", JoinType.LEFT);
				return cb.or(cb.like(q.get("subject"), "%" + kw + "%"), cb.like(q.get("content"), "%" + kw + "%"),
						cb.like(u1.get("username"), "%" + kw + "%"), cb.like(a.get("content"), "%" + kw + "%"),
						cb.like(u2.get("username"), "%" + kw + "%"));
			}
		};
	}

	public Page<Sale> getList(int page, String kw) {
		List<Sort.Order> sorts = new ArrayList<>();
		sorts.add(Sort.Order.desc("createDate"));
		Pageable pageable = PageRequest.of(page, 12, Sort.by(sorts));
		return this.saleRepository.findAllByKeyword(kw, pageable);
	}

	public void create(String subject, String content, SiteUser user, Category category, String price,
			List<UploadFile> imageFiles) {
		Sale q = new Sale();
		q.setSubject(subject);
		q.setContent(content);
		q.setCreateDate(LocalDateTime.now());
		q.setAuthor(user);
		q.setCategory(category);
		q.setPrice(price);
		q.setImageFiles(imageFiles);
		this.saleRepository.save(q);
	}

	public void modify(Sale sale, String subject, String content, Category category, String price) {
		sale.setSubject(subject);
		sale.setContent(content);
		sale.setModifyDate(LocalDateTime.now());
		sale.setCategory(category);
		sale.setPrice(price);

		this.saleRepository.save(sale);
	}

	public Sale getSale(Integer id) {
		Optional<Sale> sale = this.saleRepository.findById(id);
		if (sale.isPresent()) {
			return sale.get();
		} else {
			throw new DataNotFoundException("sale not found");
		}
	}

	public List<Sale> getSaleByCategory(Category category) {
		List<Sale> question = this.saleRepository.findByCategory(category);
		return question;
	}

	public void delete(Sale sale) {
		this.saleRepository.delete(sale);
	}

	public void vote(Sale sale, SiteUser siteUser) {
		sale.getVoter().add(siteUser);
		this.saleRepository.save(sale);
	}

	@Transactional
	public int updateView(int id) {
		return saleRepository.updateView(id);
	}

	public Page<Sale> getList2(int page, Category category) {
		List<Sort.Order> sorts = new ArrayList<>();
		sorts.add(Sort.Order.desc("createDate"));
		Pageable pageable = PageRequest.of(page, 12, Sort.by(sorts));
		return this.saleRepository.findAllByCategory(pageable, category);
	}

}
