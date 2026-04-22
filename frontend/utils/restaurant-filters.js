export function buildCategoryOptions(restaurants = [], previousCategories = [], selectedCategory = '') {
  const currentCategories = Array.from(
    new Set(
      (restaurants || [])
        .map((item) => (item && typeof item.category === 'string' ? item.category.trim() : ''))
        .filter(Boolean)
    )
  );

  if (!selectedCategory) {
    return currentCategories;
  }

  const mergedCategories = Array.from(
    new Set(
      [...(previousCategories || []), ...currentCategories]
        .map((item) => (typeof item === 'string' ? item.trim() : ''))
        .filter(Boolean)
    )
  );

  if (!mergedCategories.includes(selectedCategory)) {
    mergedCategories.unshift(selectedCategory);
  }

  return mergedCategories;
}
