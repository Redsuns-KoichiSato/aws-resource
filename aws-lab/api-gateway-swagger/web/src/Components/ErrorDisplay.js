import { Alert }from 'react-bootstrap'

const ErrorDisplay = props => {
    return (
      <Alert variant="danger" >{props.error.message}</Alert>
    )
}

export default ErrorDisplay