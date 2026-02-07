using Avalonia.Controls;
using Avalonia.Input;
using Avalonia.Interactivity;
using HelixHub.ViewModels;

namespace HelixHub.Views;

public partial class MainWindow : Window
{
    public MainWindow()
    {
        InitializeComponent();
        
        // Handle Enter key in input
        var inputBox = this.FindControl<TextBox>("InputTextBox");
        if (inputBox != null)
        {
            inputBox.KeyDown += (s, e) =>
            {
                if (e.Key == Key.Enter && DataContext is MainWindowViewModel vm)
                {
                    vm.SendMessageCommand.Execute(null);
                }
            };
        }
        
        // Auto-scroll logs
        var logViewer = this.FindControl<ScrollViewer>("LogScrollViewer");
        if (logViewer != null && DataContext is MainWindowViewModel viewModel)
        {
            viewModel.Logs.CollectionChanged += (s, e) =>
            {
                logViewer.ScrollToEnd();
            };
        }
        
        // Auto-scroll chat
        var chatViewer = this.FindControl<ScrollViewer>("ChatScrollViewer");
        if (chatViewer != null && DataContext is MainWindowViewModel vm2)
        {
            vm2.ChatMessages.CollectionChanged += (s, e) =>
            {
                chatViewer.ScrollToEnd();
            };
        }
    }
    
    protected override void OnDataContextChanged(EventArgs e)
    {
        base.OnDataContextChanged(e);
        
        if (DataContext is MainWindowViewModel vm)
        {
            vm.Logs.CollectionChanged += (s, args) =>
            {
                var logViewer = this.FindControl<ScrollViewer>("LogScrollViewer");
                logViewer?.ScrollToEnd();
            };
            
            vm.ChatMessages.CollectionChanged += (s, args) =>
            {
                var chatViewer = this.FindControl<ScrollViewer>("ChatScrollViewer");
                chatViewer?.ScrollToEnd();
            };
        }
    }
}
