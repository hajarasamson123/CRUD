package com.javastore.javastore.controller;

import java.io.InputStream;
import java.nio.file.*;
import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.javastore.javastore.models.ProductDto;
import com.javastore.javastore.models.Products;
import com.javastore.javastore.services.ProductRepository;

import jakarta.validation.Valid;

@Controller
@RequestMapping("/products")
public class ProductController {
    @Autowired
    private ProductRepository repo;

    @GetMapping({"", "/"})
    public String showProductList(Model model) {
        List<Products> products = repo.findAll(Sort.by(Sort.Direction.DESC, "id"));
        model.addAttribute("products", products);
        return "products/index";
    }

    @GetMapping("/create")
    public String showCreatePage(Model model) {
        ProductDto productDto = new ProductDto();
        model.addAttribute("productDto", productDto);
        return "products/createProduct";
    }

    @PostMapping("/create")
    public String createProduct(@Valid @ModelAttribute ProductDto productDto, BindingResult result){
        if (productDto.getImageFile().isEmpty()) {
            result.addError(new FieldError("productDto", "imageFile", "Image is required"));
        }
        if (result.hasErrors()) {
            return "products/createProduct";
        }

        MultipartFile imageFile = productDto.getImageFile();
        Date createdAt = new Date();
        String fileName = createdAt.getTime() + "_" + imageFile.getOriginalFilename();

        try {
            String uploadDir = "public/images/";
            Path uploadPath = Paths.get(uploadDir);

            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            try (InputStream inputStream = imageFile.getInputStream()) {
                Files.copy(inputStream, Paths.get(uploadDir + fileName), StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (Exception e) {
            System.out.println("Exception caught: " + e);
        }

        Products product = new Products();
        product.setName(productDto.getName());
        product.setBrand(productDto.getBrand());
        product.setCategory(productDto.getCategory());
        product.setPrice(productDto.getPrice());
        product.setDescription(productDto.getDescription());
        product.setCreatedAt(createdAt);
        product.setImageFileName(fileName);

        repo.save(product);

        return "redirect:/products";
    }

    @GetMapping("/edit")
    public String showEditPage(Model model, @RequestParam int id) {
        try {
            Products product = repo.findById(id).get();
            
            ProductDto productDto = new ProductDto();
            productDto.setName(product.getName());
            productDto.setBrand(product.getBrand());
            productDto.setCategory(product.getCategory());
            productDto.setPrice(product.getPrice());
            productDto.setDescription(product.getDescription());
            
            // Add both product and productDto to model
            model.addAttribute("product", product);
            model.addAttribute("productDto", productDto);
            
            return "products/editProduct";
        } catch (Exception e) {
            System.out.println("Exception caught: " + e.getMessage());
            return "redirect:/products";
        }
    }

    @PostMapping("/edit")
    public String updateProduct(
        @RequestParam int id, 
        @Valid @ModelAttribute ProductDto productDto,
        BindingResult result,
        Model model
    ) {
        if (result.hasErrors()) {
            Products product = repo.findById(id).get();
            model.addAttribute("product", product);
            return "products/editProduct";
        }
        
        try {
            Products product = repo.findById(id).get();
            if (!productDto.getImageFile().isEmpty()) {
                String uploadDir = "public/images/";
                Path oldImagePath = Paths.get(uploadDir + product.getImageFileName());

                try {
                    Files.delete(oldImagePath);
                } catch (Exception e) {
                    System.out.println("Exception caught: " + e.getMessage());
                }

                // Save the new file
                MultipartFile image = productDto.getImageFile();
                Date createdAt = new Date();
                String imageName = createdAt.getTime() + "_" + image.getOriginalFilename();

                try(InputStream inputStream = image.getInputStream()) {
                    Files.copy(inputStream, Paths.get(uploadDir + imageName), StandardCopyOption.REPLACE_EXISTING);
                }
                product.setImageFileName(imageName);
            }
            
            product.setName(productDto.getName());
            product.setBrand(productDto.getBrand());
            product.setCategory(productDto.getCategory());
            product.setPrice(productDto.getPrice());
            product.setDescription(productDto.getDescription());

            repo.save(product);

        } catch (Exception e) {
            System.out.println("Exception caught: " + e);
        }

        return "redirect:/products";
    }

    @GetMapping("/delete")
    public String deleteProduct(@RequestParam int id) {
        try {
            Products product = repo.findById(id).get();
            String uploadDir = "public/images/";
            Path imagePath = Paths.get(uploadDir + product.getImageFileName());
            
            try {
                Files.delete(imagePath);
            } catch (Exception e) {
                System.out.println("Exception deleting image: " + e.getMessage());
            }
            
            repo.deleteById(id);
        } catch (Exception e) {
            System.out.println("Exception caught: " + e.getMessage());
        }
        
        return "redirect:/products";
    }

}
