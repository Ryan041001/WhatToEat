export function buildRatingStars(ratingScore) {
  const safeRating = Number.isFinite(Number(ratingScore)) ? Number(ratingScore) : 0;

  return Array.from({ length: 5 }, (_, index) => {
    const diff = safeRating - index;
    let fill = 'empty';

    if (diff >= 1) {
      fill = 'full';
    } else if (diff >= 0.5) {
      fill = 'half';
    }

    return {
      id: index + 1,
      fill
    };
  });
}
