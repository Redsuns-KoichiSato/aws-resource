import {useEffect, useState} from 'react';
import { Modal, Button } from 'react-bootstrap';
import LoadingSpinner from '../LoadingSpinner';

export default (props) => {
  const handleClose = () => {props.setShow(false)}

  return (
    <Modal show={props.show} onHide={handleClose}>
      <Modal.Header closeButton>
          <Modal.Title>Listen To The Note</Modal.Title>
        </Modal.Header>
        <Modal.Body>
        {(!props.url) ? <LoadingSpinner /> : <audio controls src={props.url}>Your browser does not support the<code>audio</code> element.</audio>}
        </Modal.Body>
        <Modal.Footer>
          <Button variant="primary" onClick={handleClose}>
            Close
          </Button>
        </Modal.Footer>
    </Modal>
  )
}