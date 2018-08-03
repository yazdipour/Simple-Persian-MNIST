using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Net.Http;
using System.Net.Http.Headers;
using System.Threading.Tasks;
using Microsoft.Graphics.Canvas;
using Windows.Foundation;
using Windows.Storage;
using Windows.UI;
using Windows.UI.Core;
using Windows.UI.Input.Inking;
using Windows.UI.Xaml;
using Windows.UI.Xaml.Controls;

namespace UWP_Persian_MNIST
{
    public sealed partial class MainPage : Page
    {
        private InkPresenter _inkPresenter;
        private string fileName = "mnix.bmp";
        private string url = "https://southcentralus.api.cognitive.microsoft.com/customvision/v2.0/Prediction/69255a21-8137-401e-96db-8b28f5097690/image?iterationId=a0c48227-e2f3-461e-8566-0a66aee2e882";

        public MainPage()
        {
            this.InitializeComponent();

            _inkPresenter = canvas.InkPresenter;
            _inkPresenter.InputDeviceTypes =
                CoreInputDeviceTypes.Mouse | CoreInputDeviceTypes.Pen | CoreInputDeviceTypes.Touch;
            var defaultAttributes = _inkPresenter.CopyDefaultDrawingAttributes();
            defaultAttributes.Size = new Size(20, 20);
            _inkPresenter.UpdateDefaultDrawingAttributes(defaultAttributes);

        }

        private void Clear_Click(object sender, RoutedEventArgs e)
        {
            _inkPresenter.StrokeContainer.Clear();
        }

        private async void Detect_Click(object sender, RoutedEventArgs e)
        {
            await Save();
            Read();
        }

        private async Task Save()
        {
            StorageFolder storageFolder = KnownFolders.PicturesLibrary;
            var file = await storageFolder.CreateFileAsync(fileName,
                CreationCollisionOption.ReplaceExisting);

            CanvasDevice device = CanvasDevice.GetSharedDevice();
            CanvasRenderTarget renderTarget = new CanvasRenderTarget(device,
                (int)canvas.ActualWidth, (int)canvas.ActualHeight, 10);
            using (var ds = renderTarget.CreateDrawingSession())
            {
                ds.Clear(Colors.White);
                ds.DrawInk(canvas.InkPresenter.StrokeContainer.GetStrokes());
            }
            using (var fileStream = await file.OpenAsync(FileAccessMode.ReadWrite))
            {
                await renderTarget.SaveAsync(fileStream, CanvasBitmapFileFormat.Bmp, 1f);
            }
        }

        private async void Read()
        {
            textBlock.Text = "";
            try
            {
                StorageFile file = await KnownFolders.PicturesLibrary.GetFileAsync(fileName);
                using (var stream = await file.OpenReadAsync())
                {
                    var client = new HttpClient();
                    client.DefaultRequestHeaders.Add("Prediction-Key", "38a82ee28b694cd4ab3678e204120ceb");
                    using (var content = new StreamContent(stream.AsStream()))
                    {
                        content.Headers.ContentType = new MediaTypeHeaderValue("application/octet-stream");
                        HttpResponseMessage response = await client.PostAsync(url, content);
                        var result = await response.Content.ReadAsStringAsync();
                        var model = Newtonsoft.Json.JsonConvert.DeserializeObject<CVModel>(result);
                        if (model.Predictions == null)
                        {
                            textBlock.Text = "Error";
                            return;
                        }
                        List<Prediction> predictions = new List<Prediction>(model.Predictions);
                        predictions = predictions.OrderBy(o => o.Probability).ToList();
                        textBlock.Text = $"{model.Predictions[0].tagName} with {model.Predictions[0].Probability*100}%";
                    }
                }
            }
            catch (Exception ex)
            {
                textBlock.Text = ex.Message;
            }
        }
    }
}