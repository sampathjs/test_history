import * as Styles from './AppHeader.styles';
import { ReactComponent as Logo } from './assets/logo.svg';

export const AppHeader = () => {
  return (
    <Styles.Header>
      <Styles.HomeLink to="/">
        <Logo />
      </Styles.HomeLink>
      <span>{process.env.REACT_APP_VERSION}</span>
    </Styles.Header>
  );
};
