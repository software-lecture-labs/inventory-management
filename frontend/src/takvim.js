import React from "react";
import FullCalendar from "@fullcalendar/react";
import dayGridPlugin from "@fullcalendar/daygrid";
import timeGridPlugin from "@fullcalendar/timegrid";
import interactionPlugin from "@fullcalendar/interaction";

function RezervasyonTakvimi() {
  // Örnek rezervasyon verileri
  const events = [
    {
      id: 1,
      title: "Laptop Rezervasyonu",
      start: "2025-09-10T10:00:00",
      end: "2025-09-10T12:00:00",
    },
    {
      id: 2,
      title: "Projeksiyon Cihazı",
      start: "2025-09-12T14:00:00",
      end: "2025-09-12T16:00:00",
    },
  ];

  const handleDateClick = (info) => {
    alert("Seçilen tarih: " + info.dateStr);
  };

  return (
    <div>
      <h2 style={{ textAlign: "center", margin: "20px" }}>
        Rezervasyon Takvimi
      </h2>
      <FullCalendar
        plugins={[dayGridPlugin, timeGridPlugin, interactionPlugin]}
        initialView="dayGridMonth"
        headerToolbar={{
          left: "prev,next today",
          center: "title",
          right: "dayGridMonth,timeGridWeek,timeGridDay",
        }}
        events={events}
        dateClick={handleDateClick}
        editable={true}
        selectable={true}
        height="80vh"
      />
    </div>
  );
}

export default RezervasyonTakvimi;
