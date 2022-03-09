import { useState, useEffect } from 'react';
import {Container, Form, Row, Col, Button, InputGroup} from 'react-bootstrap';
import LoadingSpinner from '../LoadingSpinner';

let NoteEntry = (props) => {
  
  const [noteText, setNoteText] = useState("")
  
  useEffect(() => {
    if (props.selNote.Note){
      setNoteText(props.selNote.Note)
    } else {
      setNoteText("")
    }
  }, [props.selNote]) 

  const addNote = (e) => {
    e.preventDefault()
    if (noteText != "") {
      if (props.selNote.NoteId != "") {
        props.noteFunctions.updateNote(props.selNote, noteText)
        setNoteText("")
      } else {
        props.noteFunctions.addNote(noteText)
        setNoteText("")
      }
    }
  }

  return (
    <Container>
      <Form onSubmit={(e) => addNote(e)}>
        <Row>
          <Col sm="11"> 
            <Form.Group controlId="noteEditor">
              <Form.Label column sm="1" srOnly>
                Note
              </Form.Label>
              <InputGroup>
                <InputGroup.Prepend>
                <InputGroup.Text>Note:</InputGroup.Text>
                </InputGroup.Prepend>
                <Form.Control value={noteText} onChange={e => setNoteText(e.target.value)} placeholder="Note Text...."></Form.Control>
              </InputGroup>
            </Form.Group>
          </Col>
          <Col sm="1" align="left">
            <Button variant="outline-primary" type="submit">{(props.updating) ? <LoadingSpinner /> : (props.selNote.NoteId == "") ? 'Add' : 'Update'}</Button>
          </Col>
        </Row>
      </Form>
    </Container>
  )
}

export default NoteEntry;