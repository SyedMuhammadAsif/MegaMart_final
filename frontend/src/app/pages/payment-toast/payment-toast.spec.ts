import { ComponentFixture, TestBed } from '@angular/core/testing';

import { PaymentToastComponent } from './payment-toast';

describe('PaymentToast', () => {
  let component: PaymentToastComponent;
  let fixture: ComponentFixture<PaymentToastComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PaymentToastComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(PaymentToastComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
