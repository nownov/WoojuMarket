package com.mysite.wooju.sale;

import com.mysite.wooju.answer.AnswerForm;
import com.mysite.wooju.user.SiteUser;
import com.mysite.wooju.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;
import com.mysite.wooju.category.Category;
import com.mysite.wooju.category.CategoryService;
import javax.validation.Valid;
import java.io.IOException;
import java.net.MalformedURLException;
import java.security.Principal;
import java.util.List;

@RequestMapping("/sale")
@RequiredArgsConstructor
@Controller

public class SaleController {

	private final SaleService saleService;
	private final UserService userService;
	private final CategoryService categoryService;
	FileStore fileStore = new FileStore();

	@RequestMapping("/list")
	public String list(Model model, @RequestParam(value = "page", defaultValue = "0") int page,
			@RequestParam(value = "kw", defaultValue = "") String kw) {
		Page<Sale> paging = this.saleService.getList(page, kw);
		model.addAttribute("paging", paging);
		model.addAttribute("kw", kw);
		return "sale_list";
	}

	@RequestMapping(value = "/category/{id}")
	public String categoryDetail(Model model, @PathVariable("id") Integer id) {

		Category category = this.categoryService.getCategory2(id);
		model.addAttribute("category", category);
		return "category_detail";
	}

	@GetMapping("/category/{id}")
	public String findcategoryByCategoryid2(Model model, @PathVariable("id") Integer id,
			@RequestParam(value = "page", defaultValue = "0") int page) {
		Category category = this.categoryService.getCategory2(id);
		Page<Sale> paging = this.saleService.getList2(page, category);
		model.addAttribute("paging", paging);
		return "category_detail";
	}

	@RequestMapping(value = "/detail/{id}")
	public String detail(Model model, @PathVariable("id") Integer id, AnswerForm answerForm) {
		Sale sale = this.saleService.getSale(id);
		saleService.updateView(id);
		model.addAttribute("sale", sale);
		return "sale_detail";
	}

	@PreAuthorize("isAuthenticated()")
	@GetMapping("/create")
	public String saleCreate(SaleForm saleForm) {
		return "sale_form";
	}

	@PreAuthorize("isAuthenticated()")
	@PostMapping("/create")
	public String saleCreate(@ModelAttribute("saleForm") @Valid SaleForm saleForm, BindingResult bindingResult,
			Principal principal, Object Category) throws IOException {
		if (bindingResult.hasErrors()) {
			return "sale_form";
		}

		Sale sale = new Sale();
		List<UploadFile> imageFiles = fileStore.storeFiles(saleForm.getImageFiles());
		sale.setImageFiles(imageFiles);
		SiteUser siteUser = this.userService.getUser(principal.getName());
		this.saleService.create(saleForm.getSubject(), saleForm.getContent(), siteUser, saleForm.getCategory(),
				saleForm.getPrice(), imageFiles);
		return "redirect:/sale/list";
	}

	@ModelAttribute("categoryCodes")
	public List<Category> categoryCodes() {
		List<Category> categoryCodes = categoryService.getCategory();
		return categoryCodes;
	}

	@ModelAttribute
	public String menu(Model model) {
		List<Category> categoryList = categoryService.getCategory();
		model.addAttribute("categories", categoryList);

		return "/list";
	}

	@ResponseBody
	@GetMapping("/images/{filename}")
	public Resource downloadImage(@PathVariable String filename) throws MalformedURLException {
		return new UrlResource("file:" + fileStore.getFullPath(filename));
	}

	@PreAuthorize("isAuthenticated()")
	@GetMapping("/modify/{id}")
	public String saleModify(SaleForm saleForm, @PathVariable("id") Integer id, Principal principal) {
		Sale sale = this.saleService.getSale(id);
		if (!sale.getAuthor().getUsername().equals(principal.getName())) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "수정권한이 없습니다.");
		}
		saleForm.setSubject(sale.getSubject());
		saleForm.setContent(sale.getContent());
		saleForm.setCategory(sale.getCategory());
		saleForm.setPrice(sale.getPrice());

		return "sale_form";
	}

	@PreAuthorize("isAuthenticated()")
	@PostMapping("/modify/{id}")
	public String saleModify(@Valid SaleForm saleForm, BindingResult bindingResult, Principal principal,
			@PathVariable("id") Integer id) {
		if (bindingResult.hasErrors()) {
			return "sale_form";
		}
		Sale sale = this.saleService.getSale(id);
		if (!sale.getAuthor().getUsername().equals(principal.getName())) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "수정권한이 없습니다.");
		}
		this.saleService.modify(sale, saleForm.getSubject(), saleForm.getContent(), saleForm.getCategory(),
				saleForm.getPrice());
		return String.format("redirect:/sale/detail/%s", id);
	}

	@PreAuthorize("isAuthenticated()")
	@GetMapping("/delete/{id}")
	public String saleDelete(Principal principal, @PathVariable("id") Integer id) {
		Sale sale = this.saleService.getSale(id);
		if (!sale.getAuthor().getUsername().equals(principal.getName())) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "삭제권한이 없습니다.");
		}
		this.saleService.delete(sale);
		return "redirect:/sale/list";
	}

	@PreAuthorize("isAuthenticated()")
	@GetMapping("/vote/{id}")
	public String saleVote(Principal principal, @PathVariable("id") Integer id) {
		Sale sale = this.saleService.getSale(id);
		SiteUser siteUser = this.userService.getUser(principal.getName());
		this.saleService.vote(sale, siteUser);
		return String.format("redirect:/sale/detail/%s", id);
	}

}
