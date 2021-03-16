package com.my.grocery.service;

import com.my.grocery.dto.ingredient.IngredientRequestDto;
import com.my.grocery.dto.ingredient.IngredientResponseDto;
import com.my.grocery.dto.recipe.RecipeItemResponseDto;
import com.my.grocery.dto.recipe.RecipeRequestDto;
import com.my.grocery.dto.recipe.RecipeResponseDto;
import com.my.grocery.model.Ingredient;
import com.my.grocery.model.Party;
import com.my.grocery.model.Recipe;
import com.my.grocery.model.RecipeItem;
import com.my.grocery.repository.IIngredientRepository;
import com.my.grocery.repository.IPartyRepository;
import com.my.grocery.repository.IRecipeItemRepository;
import com.my.grocery.repository.IRecipeRepository;
import javassist.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class RecipeService {
    @Autowired
    private IPartyRepository partyRepository;
    @Autowired
    private IRecipeRepository recipeRepository;
    @Autowired
    private IRecipeItemRepository recipeItemRepository;
    @Autowired
    private IIngredientRepository ingredientRepository;

    //Create new ingredient
    @Transactional(isolation = Isolation.READ_COMMITTED, propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public RecipeResponseDto createRecipe(RecipeRequestDto req) {
        Assert.notNull(req.getRecipeItemList(), "Recipe ingredients is required.");
        Assert.notNull(req.getRecipeName(), "Recipe name is required.");
        Assert.notNull(req.getPartyId(), "Party Id is required.");

        Party party = partyRepository.findById(req.getPartyId());
        Recipe recipe = new Recipe();
        recipe.setRecipeName(req.getRecipeName());
        recipe.setRecipePhoto(req.getRecipePhoto());
        recipe.setParty(party);
        recipeRepository.save(recipe);
        Optional<Recipe> existingRecipe = recipeRepository.findById(recipe.getRecipeId());

        if (req.getRecipeItemList() != null) {
            for (var i = 0; i < req.getRecipeItemList().size(); i++) {
                RecipeItem item = new RecipeItem();
                Optional<Ingredient> ingredient = ingredientRepository.findById(req.getRecipeItemList().get(i).getIngredientId());
                item.setIngredient(ingredient.get());
                item.setItemQuantity(req.getRecipeItemList().get(i).getItemQuantity());
                item.setRecipe(existingRecipe.get());
                existingRecipe.get().addRecipeItem(item);
                recipeItemRepository.save(item);
            }
        }
        recipeRepository.save(existingRecipe.get());
        List<RecipeItem> recipeItemList = recipeItemRepository.getRecipeItemList(existingRecipe.get().getRecipeId());
        List<RecipeItemResponseDto> dtos = new ArrayList<>();
        for (int i = 0; i < recipeItemList.size(); i++) {
            RecipeItemResponseDto dto = new RecipeItemResponseDto();
            dto.setIngredientId(recipeItemList.get(i).getIngredient().getIngredientId());
            dto.setRecipeId(recipeItemList.get(i).getRecipe().getRecipeId());
            dto.setItemId(recipeItemList.get(i).getItemId());
            dto.setItemQuantity(recipeItemList.get(i).getItemQuantity());
            dtos.add(dto);
        }
        return new RecipeResponseDto(existingRecipe.get(), dtos);
    }

    // Get list of recipes
    @Transactional(isolation = Isolation.READ_COMMITTED, propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public List<RecipeResponseDto> getListOfRecipes() {

        List<Recipe> recipes = recipeRepository.findAll();
        List<RecipeResponseDto> ls = new ArrayList<>();
        for (int i = 0; i < recipes.size(); i++) {
            RecipeResponseDto dto = new RecipeResponseDto();
            dto.setRecipeId(recipes.get(i).getRecipeId());
            dto.setRecipeName(recipes.get(i).getRecipeName());
            dto.setRecipePhoto(recipes.get(i).getRecipePhoto());
            dto.setPartyId(recipes.get(i).getParty().getPartyId());
            List<RecipeItem> recipeItemList = recipes.get(i).getItems();
            List<RecipeItemResponseDto> dtos = new ArrayList<>();

            for (var j = 0; j < recipeItemList.size(); j++) {
                RecipeItemResponseDto resDto = new RecipeItemResponseDto();
                resDto.setIngredientId(recipeItemList.get(j).getIngredient().getIngredientId());
                resDto.setRecipeId(recipeItemList.get(j).getRecipe().getRecipeId());
                resDto.setItemId(recipeItemList.get(j).getItemId());
                resDto.setItemQuantity(recipeItemList.get(j).getItemQuantity());
                dtos.add(resDto);

            }
            dto.setRecipeItemList(dtos);
            ls.add(dto);
        }

        return ls;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED, propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public void deleteRecipe(long id) throws Exception {

        Optional<Recipe> deleteRecipe = recipeRepository.findById(id);

        if (deleteRecipe == null) throw new NotFoundException("Recipe is not invalid");
        List<RecipeItem> list = recipeItemRepository.getRecipeItemByRecipeId(id);
        for (var i : list) {
            recipeItemRepository.delete(i);
        }
        deleteRecipe.get().getItems().removeAll(deleteRecipe.get().getItems());
        recipeRepository.deleteById(id);
    }

}