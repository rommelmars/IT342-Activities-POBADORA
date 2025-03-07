$(document).ready(function() {
    var token = $("meta[name='_csrf']").attr("content");
    var header = $("meta[name='_csrf_header']").attr("content");
    
    $(document).ajaxSend(function(e, xhr, options) {
        xhr.setRequestHeader(header, token);
    });
});

function showAddPopup() {
    $('#overlay').show();
    $('#addPopup').show();
}

function closeAddPopup() {
    $('#overlay').hide();
    $('#addPopup').hide();
}

$('#addForm').submit(function(e) {
    e.preventDefault();
    $.ajax({
        url: '/contacts/add',
        type: 'POST',
        data: {
            name: $('#addName').val(),
            email: $('#addEmail').val(),
            phone: $('#addPhone').val()
        },
        success: function() { location.reload(); },
        error: function(err) {
            console.error("Error adding contact:", err.responseJSON);
            alert(err.responseJSON ? err.responseJSON.message : 'Failed to add contact.');
        }
    });
});

function showEditPopup(resourceName, name, email, phone) {
    $('#overlay').show();
    $('#editResourceName').val(resourceName);
    $('#editName').val(name);
    $('#editEmail').val(email);
    $('#editPhone').val(phone);
    $('#editPopup').addClass('show').show();
}


function closeEditPopup() {
    $('#overlay').hide();
    $('#editPopup').removeClass('show').hide();
}


$('#editForm').submit(function(e) {
    e.preventDefault();
    const resourceName = $('#editResourceName').val();
    const name = $('#editName').val().trim();
    const email = $('#editEmail').val().trim();
    const phone = $('#editPhone').val().trim();

    if (!resourceName) {
        alert('Invalid contact. Please refresh the page.');
        return;
    }

    if (!name || !email || !phone) {
        alert('All fields are required.');
        return;
    }

    $.ajax({
        url: '/contacts/edit',
        type: 'POST',
        data: {
            resourceName: resourceName,
            name: name,
            email: email,
            phone: phone
        },
        success: function() { location.reload(); },
        error: function(err) {
            console.error("Error updating contact:", err.responseJSON);
            alert(err.responseJSON ? err.responseJSON.message : 'Failed to update contact.');
        }
    });
});

function deleteContact(resourceName) {
    if (confirm('Are you sure you want to delete this contact?')) {
        $.ajax({
            url: '/contacts/delete',
            type: 'POST',
            data: { resourceName: resourceName },
            success: function() { location.reload(); },
            error: function(err) {
                console.error("Error deleting contact:", err.responseJSON);
                alert(err.responseJSON ? err.responseJSON.message : 'Failed to delete contact.');
            }
        });
    }
}
