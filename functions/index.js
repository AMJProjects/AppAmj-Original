const { onDocumentCreated } = require("firebase-functions/v2/firestore");
const { initializeApp } = require("firebase-admin/app");
const { getMessaging } = require("firebase-admin/messaging");

initializeApp();

// Notificação para novos escopos pendentes (somente se status for "pendente" e origem não for "movido")
exports.notificarNovoEscopo = onDocumentCreated("escoposPendentes/{escopoId}", (event) => {
  const snap = event.data;
  const novoEscopo = snap.data();
  const escopoId = event.params.escopoId;

  const status = novoEscopo?.status?.toLowerCase?.() || "pendente";
  const origem = novoEscopo?.origem?.toLowerCase?.() || "";

  // Ignora se o status não for pendente ou se o documento foi movido
  if (status !== "pendente" || origem === "movido") {
    console.log(`Ignorando escopo ${escopoId} (status: ${status}, origem: ${origem})`);
    return null;
  }

  const nomeEscopo = novoEscopo?.nome || "Novo Escopo Criado";

  const payload = {
    data: {
      title: "Novo Escopo Disponível",
      body: `Escopo: ${nomeEscopo}`,
      escopoId: escopoId
    },
    topic: "escopos"
  };

  return getMessaging().send(payload)
    .then((response) => {
      console.log("Notificação de novo escopo enviada com sucesso:", response);
    })
    .catch((error) => {
      console.error("Erro ao enviar notificação de novo escopo:", error);
    });
});

// Notificação para escopos concluídos
exports.notificarEscopoConcluido = onDocumentCreated("escoposConcluidos/{escopoId}", (event) => {
  const snap = event.data;

  if (!snap) {
    console.log("Documento de escopo concluído não encontrado.");
    return null;
  }

  const escopo = snap.data();
  const escopoId = event.params.escopoId;
  const nomeEscopo = escopo?.nome || "Escopo sem nome";

  const payload = {
    data: {
      title: "✅ Escopo Finalizado",
      body: `O escopo "${nomeEscopo}" foi marcado como concluído.`,
      escopoId: escopoId
    },
    topic: "escopos"
  };

  console.log("Enviando notificação de escopo concluído:", payload);

  return getMessaging().send(payload)
    .then((response) => {
      console.log("Notificação de conclusão enviada com sucesso:", response);
    })
    .catch((error) => {
      console.error("Erro ao enviar notificação de conclusão:", error);
    });
});
