import { createBrowserRouter } from 'react-router';
import { Layout } from './components/Layout';
import { Home } from './pages/Home';
import { SpinWheel } from './pages/SpinWheel';
import { CardSwipe } from './pages/CardSwipe';
import { RestaurantList } from './pages/RestaurantList';
import { Mine } from './pages/Mine';

export const router = createBrowserRouter([
  {
    path: '/',
    Component: Layout,
    children: [
      { index: true, Component: Home },
      { path: 'spin', Component: SpinWheel },
      { path: 'swipe', Component: CardSwipe },
      { path: 'restaurants', Component: RestaurantList },
      { path: 'mine', Component: Mine },
    ],
  },
]);
