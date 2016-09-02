import { connect } from 'react-redux';
import YBPanelItem from '../components/YBPanelItem';

const mapStateToProps = (state) => {
  return {
    customer: state.customer
  };
}

export default connect(mapStateToProps)(YBPanelItem);
