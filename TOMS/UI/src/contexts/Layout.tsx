import { createContext, ReactNode, useContext, useState } from 'react';

type ProviderProps = {
  children: ReactNode;
};

type Layout = {
  filter: {
    isPanelExpanded: boolean;
    onPanelToggle: () => void;
    isToggleHover: boolean;
    onToggleHover: (isHover: boolean) => void;
  };
};

const LayoutContext = createContext<Layout>({
  filter: {
    isPanelExpanded: true,
    onPanelToggle: () => undefined,
    isToggleHover: false,
    onToggleHover: (isHover) => undefined,
  },
});

export const LayoutProvider = ({ children }: ProviderProps) => {
  const [isFilterPanelExpanded, setIsFilterPanelExpanded] = useState(true);
  const [isFilterToggleHover, setIsFilterToggleHover] = useState(false);

  const handleFilterPanelToggle = () =>
    setIsFilterPanelExpanded((isExpanded) => !isExpanded);

  const handleFilterToggleHover = (isHover: boolean) =>
    setIsFilterToggleHover(isHover);

  return (
    <LayoutContext.Provider
      value={{
        filter: {
          isPanelExpanded: isFilterPanelExpanded,
          onPanelToggle: handleFilterPanelToggle,
          isToggleHover: isFilterToggleHover,
          onToggleHover: handleFilterToggleHover,
        },
      }}
    >
      {children}
    </LayoutContext.Provider>
  );
};

export const useLayoutContext = () => useContext(LayoutContext);
