import { Button } from 'components/Button';
import { ExportIcon, FilterHideIcon, FilterShowIcon } from 'components/Icon';
import { useLayoutContext } from 'contexts';

import * as Styles from './Header.styles';

export const Header = () => {
  const { filter } = useLayoutContext();

  const handleToggleMouseEnter = () => filter.onToggleHover(true);
  const handleToggleMouseOut = () => filter.onToggleHover(false);

  return (
    <Styles.Wrapper>
      {filter.isPanelExpanded ? (
        <Button
          color="secondary"
          endIcon={<FilterHideIcon />}
          onClick={filter.onPanelToggle}
          onMouseEnter={handleToggleMouseEnter}
          onMouseLeave={handleToggleMouseOut}
        >
          Filter
        </Button>
      ) : (
        <Button
          color="primary"
          startIcon={<FilterShowIcon />}
          onClick={filter.onPanelToggle}
          onMouseEnter={handleToggleMouseEnter}
          onMouseLeave={handleToggleMouseOut}
        >
          Filter
        </Button>
      )}
      <Styles.Search>
        <Styles.SearchInput placeholder="Search open orders" />
        <Styles.SearchIcon />
      </Styles.Search>
      <Button color="secondary" variant="outlined" startIcon={<ExportIcon />}>
        Export
      </Button>
    </Styles.Wrapper>
  );
};
